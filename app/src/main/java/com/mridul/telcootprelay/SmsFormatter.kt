package com.mridul.telcootprelay

import java.text.SimpleDateFormat
import java.util.Date
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
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.US)
        return sdf.format(Date(millis))
    }
}
