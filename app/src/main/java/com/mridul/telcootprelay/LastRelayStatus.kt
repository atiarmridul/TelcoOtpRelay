package com.mridul.telcootprelay

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RelayDeliveryResult {
    DELIVERED,
    FAILED
}

data class LastRelayStatus(
    val result: RelayDeliveryResult,
    val sender: String,
    val timestampMillis: Long
) {
    fun formatTimestamp(locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", locale).format(Date(timestampMillis))

    companion object {
        fun fromStored(
            result: String?,
            sender: String?,
            timestampMillis: Long
        ): LastRelayStatus? {
            val parsedResult = runCatching { RelayDeliveryResult.valueOf(result.orEmpty()) }
                .getOrNull()
                ?: return null
            if (timestampMillis < 0) return null

            return LastRelayStatus(
                result = parsedResult,
                sender = sender?.trim()?.takeIf(String::isNotEmpty) ?: "Unknown",
                timestampMillis = timestampMillis
            )
        }
    }
}

object LastRelayStore {
    const val RESULT_KEY = "last_relay_result"
    const val SENDER_KEY = "last_relay_sender"
    const val TIMESTAMP_KEY = "last_relay_timestamp"

    private val statusKeys = setOf(RESULT_KEY, SENDER_KEY, TIMESTAMP_KEY)

    fun save(
        preferences: SharedPreferences,
        result: RelayDeliveryResult,
        sender: String,
        timestampMillis: Long
    ) {
        preferences.edit()
            .putString(RESULT_KEY, result.name)
            .putString(SENDER_KEY, sender.trim().ifEmpty { "Unknown" })
            .putLong(TIMESTAMP_KEY, timestampMillis)
            .commit()
    }

    fun load(preferences: SharedPreferences): LastRelayStatus? = LastRelayStatus.fromStored(
        result = preferences.getString(RESULT_KEY, null),
        sender = preferences.getString(SENDER_KEY, null),
        timestampMillis = preferences.getLong(TIMESTAMP_KEY, -1L)
    )

    fun isStatusKey(key: String?): Boolean = key in statusKeys
}
