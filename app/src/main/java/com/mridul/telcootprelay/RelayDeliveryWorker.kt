package com.mridul.telcootprelay

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class RelayDeliveryWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val pendingId = inputData.getLong(RelayWorkScheduler.PENDING_ID_KEY, -1L)
        val pending = PendingRelayStore.load(applicationContext, pendingId)
            ?: return Result.success()

        if (RelayWorkPolicy.isExpired(pending.createdAtMillis, System.currentTimeMillis())) {
            RelayResultRecorder.record(
                applicationContext,
                pending,
                ChatSendResult(false, RelayFailureKind.RETRY_EXPIRED)
            )
            PendingRelayStore.delete(applicationContext, pending.id)
            return Result.success()
        }

        val sendResult = ChatSender.sendWithRetry(pending.webhook, pending.message)
        if (sendResult.delivered) {
            RelayResultRecorder.record(applicationContext, pending, sendResult)
            PendingRelayStore.delete(applicationContext, pending.id)
            return Result.success()
        }

        if (!RelayWorkPolicy.shouldRetry(sendResult)) {
            RelayResultRecorder.record(applicationContext, pending, sendResult)
            PendingRelayStore.delete(applicationContext, pending.id)
            return Result.failure()
        }
        return Result.retry()
    }
}

class PendingRelayExpiryWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val pendingId = inputData.getLong(RelayWorkScheduler.PENDING_ID_KEY, -1L)
        val pending = PendingRelayStore.load(applicationContext, pendingId)
            ?: return Result.success()
        if (!RelayWorkPolicy.isExpired(pending.createdAtMillis, System.currentTimeMillis())) {
            return Result.retry()
        }
        if (PendingRelayStore.delete(applicationContext, pending.id)) {
            RelayResultRecorder.record(
                applicationContext,
                pending,
                ChatSendResult(false, RelayFailureKind.RETRY_EXPIRED)
            )
            RelayWorkScheduler.cancelDelivery(applicationContext, pending.id)
        }
        return Result.success()
    }
}

object RelayWorkScheduler {
    internal const val PENDING_ID_KEY = "pending_relay_id"
    private const val WORK_NAME_PREFIX = "relay-delivery-"
    private const val EXPIRY_WORK_NAME_PREFIX = "relay-expiry-"
    private const val RETRY_BACKOFF_SECONDS = 30L

    fun enqueue(context: Context, pendingId: Long) {
        val request = OneTimeWorkRequestBuilder<RelayDeliveryWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(PENDING_ID_KEY, pendingId)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                RETRY_BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "$WORK_NAME_PREFIX$pendingId",
            ExistingWorkPolicy.KEEP,
            request
        )
        val expiryRequest = OneTimeWorkRequestBuilder<PendingRelayExpiryWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(PENDING_ID_KEY, pendingId)
                    .build()
            )
            .setInitialDelay(RelayWorkPolicy.MAX_RETENTION_MILLIS, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                RETRY_BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "$EXPIRY_WORK_NAME_PREFIX$pendingId",
            ExistingWorkPolicy.KEEP,
            expiryRequest
        )
    }

    fun cancelDelivery(context: Context, pendingId: Long) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork("$WORK_NAME_PREFIX$pendingId")
    }
}

object RelayWorkPolicy {
    const val MAX_RETENTION_MILLIS = 24L * 60L * 60L * 1000L

    fun isExpired(createdAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis - createdAtMillis >= MAX_RETENTION_MILLIS

    fun shouldRetry(result: ChatSendResult): Boolean {
        if (result.delivered) return false
        if (result.failureKind != RelayFailureKind.HTTP_STATUS) return true
        val status = result.httpStatusCode ?: return false
        return status == 408 || status == 429 || status in 500..599
    }
}

object RelayResultRecorder {
    fun record(context: Context, pending: PendingRelay, sendResult: ChatSendResult) {
        val preferences = context.getSharedPreferences(
            RelaySettings.PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        val result = if (sendResult.delivered) {
            RelayDeliveryResult.DELIVERED
        } else {
            RelayDeliveryResult.FAILED
        }
        runCatching { RelayHistoryStore.migrateLatestIfNeeded(context, preferences) }
        LastRelayStore.save(
            preferences = preferences,
            result = result,
            sender = pending.sender,
            timestampMillis = pending.smsTimestampMillis,
            failureKind = sendResult.failureKind,
            httpStatusCode = sendResult.httpStatusCode
        )
        runCatching {
            RelayHistoryStore.append(
                context,
                LastRelayStatus(
                    result = result,
                    sender = pending.sender,
                    timestampMillis = pending.smsTimestampMillis,
                    failureKind = sendResult.failureKind,
                    httpStatusCode = sendResult.httpStatusCode
                )
            )
        }
    }
}
