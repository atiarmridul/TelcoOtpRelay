package com.mridul.telcootprelay

import android.content.SharedPreferences
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RelayDeliveryResult {
    DELIVERED,
    FAILED
}

data class LastRelayStatus(
    val result: RelayDeliveryResult,
    val sender: String,
    val timestampMillis: Long,
    val failureKind: RelayFailureKind? = null,
    val httpStatusCode: Int? = null
) {
    fun formatTimestamp(
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String = Instant.ofEpochMilli(timestampMillis)
        .atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", locale))

    companion object {
        fun fromStored(
            result: String?,
            sender: String?,
            timestampMillis: Long,
            failureKind: String? = null,
            httpStatusCode: Int = -1
        ): LastRelayStatus? {
            val parsedResult = runCatching { RelayDeliveryResult.valueOf(result.orEmpty()) }
                .getOrNull()
                ?: return null
            if (timestampMillis < 0) return null

            return LastRelayStatus(
                result = parsedResult,
                sender = sender?.trim()?.takeIf(String::isNotEmpty) ?: "Unknown",
                timestampMillis = timestampMillis,
                failureKind = failureKind
                    ?.let { runCatching { RelayFailureKind.valueOf(it) }.getOrNull() },
                httpStatusCode = httpStatusCode.takeIf { it >= 100 }
            )
        }
    }
}

object LastRelayStore {
    const val RESULT_KEY = "last_relay_result"
    const val SENDER_KEY = "last_relay_sender"
    const val TIMESTAMP_KEY = "last_relay_timestamp"
    const val FAILURE_KIND_KEY = "last_relay_failure_kind"
    const val HTTP_STATUS_CODE_KEY = "last_relay_http_status_code"

    private val statusKeys = setOf(
        RESULT_KEY,
        SENDER_KEY,
        TIMESTAMP_KEY,
        FAILURE_KIND_KEY,
        HTTP_STATUS_CODE_KEY
    )

    fun save(
        preferences: SharedPreferences,
        result: RelayDeliveryResult,
        sender: String,
        timestampMillis: Long,
        failureKind: RelayFailureKind? = null,
        httpStatusCode: Int? = null
    ) {
        val editor = preferences.edit()
            .putString(RESULT_KEY, result.name)
            .putString(SENDER_KEY, sender.trim().ifEmpty { "Unknown" })
            .putLong(TIMESTAMP_KEY, timestampMillis)
            .remove(FAILURE_KIND_KEY)
            .remove(HTTP_STATUS_CODE_KEY)
        failureKind?.let { editor.putString(FAILURE_KIND_KEY, it.name) }
        httpStatusCode?.let { editor.putInt(HTTP_STATUS_CODE_KEY, it) }
        editor.commit()
    }

    fun load(preferences: SharedPreferences): LastRelayStatus? = LastRelayStatus.fromStored(
        result = preferences.getString(RESULT_KEY, null),
        sender = preferences.getString(SENDER_KEY, null),
        timestampMillis = preferences.getLong(TIMESTAMP_KEY, -1L),
        failureKind = preferences.getString(FAILURE_KIND_KEY, null),
        httpStatusCode = preferences.getInt(HTTP_STATUS_CODE_KEY, -1)
    )

    fun clear(preferences: SharedPreferences) {
        preferences.edit()
            .remove(RESULT_KEY)
            .remove(SENDER_KEY)
            .remove(TIMESTAMP_KEY)
            .remove(FAILURE_KIND_KEY)
            .remove(HTTP_STATUS_CODE_KEY)
            .commit()
    }

    fun isStatusKey(key: String?): Boolean = key in statusKeys
}
