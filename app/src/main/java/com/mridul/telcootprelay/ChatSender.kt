package com.mridul.telcootprelay

import java.net.HttpURLConnection
import java.net.URL

object ChatSender {
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 5000

    fun sendWithRetry(webhookUrl: String, text: String): Boolean {
        var delayMs = 0L
        repeat(3) { attempt ->
            if (delayMs > 0) Thread.sleep(delayMs)
            if (sendOnce(webhookUrl, text)) return true
            delayMs = if (attempt == 0) 1000L else 2000L
        }
        return false
    }

    private fun sendOnce(webhookUrl: String, text: String): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(webhookUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.useCaches = false

            val json = "{\"text\":\"${escapeJson(text)}\"}"
            connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            connection.responseCode in 200..299
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
}