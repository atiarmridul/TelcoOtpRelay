package com.mridul.telcootprelay

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class SmsReceiver : BroadcastReceiver() {

    companion object {
        // One shared worker; no thread is created for each incoming SMS.
        private val executor = Executors.newSingleThreadExecutor()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pending = goAsync()
        val appContext = context.applicationContext

        executor.execute {
            try {
                val prefs = appContext.getSharedPreferences(
                    RelaySettings.PREFERENCES_NAME,
                    Context.MODE_PRIVATE
                )
                val configuration = RelayConfiguration(
                    webhook = prefs.getString(RelaySettings.WEBHOOK_KEY, "")?.trim().orEmpty(),
                    mode = RelayMode.fromStored(prefs.getString(RelaySettings.MODE_KEY, null)),
                    allowedSenders = prefs.getString(RelaySettings.SENDERS_KEY, null)
                )
                if (configuration.webhook.isBlank()) return@execute

                val sms = SmsBroadcastParser.parse(intent) ?: return@execute
                val request = RelayProcessor.prepare(configuration, sms) {
                    SimNumberResolver.resolve(appContext, sms.subscriptionId)
                } ?: return@execute

                val sendResult = ChatSender.sendWithRetry(request.webhook, request.message)
                val result = if (sendResult.delivered) {
                    RelayDeliveryResult.DELIVERED
                } else {
                    RelayDeliveryResult.FAILED
                }
                runCatching { RelayHistoryStore.migrateLatestIfNeeded(appContext, prefs) }
                LastRelayStore.save(
                    preferences = prefs,
                    result = result,
                    sender = request.sender,
                    timestampMillis = request.timestampMillis,
                    failureKind = sendResult.failureKind,
                    httpStatusCode = sendResult.httpStatusCode
                )
                runCatching {
                    RelayHistoryStore.append(
                        appContext,
                        LastRelayStatus(
                            result = result,
                            sender = request.sender,
                            timestampMillis = request.timestampMillis,
                            failureKind = sendResult.failureKind,
                            httpStatusCode = sendResult.httpStatusCode
                        )
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}

object SimNumberResolver {
    fun resolve(context: Context, subscriptionId: Int?): String {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED
        ) return "Unknown"

        if (subscriptionId != null &&
            subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) {
            try {
                val number = context
                    .getSystemService(SubscriptionManager::class.java)
                    .getPhoneNumber(subscriptionId)
                    .trim()
                if (number.isNotEmpty()) return number
            } catch (_: Exception) { }
        }

        return try {
            val tm = context.getSystemService(android.telephony.TelephonyManager::class.java)
            val scoped = if (
                subscriptionId != null &&
                subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
                android.os.Build.VERSION.SDK_INT >= 24
            ) tm.createForSubscriptionId(subscriptionId) else tm
            scoped.line1Number?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
