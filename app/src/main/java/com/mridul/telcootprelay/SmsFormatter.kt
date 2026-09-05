package com.mridul.telcootprelay

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object SmsFormatter {
    fun format(
        receivingNumber: String,
        sender: String,
        timestampMillis: Long,
        body: String
    ): String = buildString {
        append("Receiving number: ").append(receivingNumber.ifBlank { "Unknown" }).append("\n")
        append("Sender: ").append(sender.ifBlank { "Unknown" }).append("\n")
        append("Timestamp: ").append(formatLocal(timestampMillis)).append("\n")
        append("Body: ").append(body)
    }

    private fun formatLocal(millis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a", Locale.US)
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
}
