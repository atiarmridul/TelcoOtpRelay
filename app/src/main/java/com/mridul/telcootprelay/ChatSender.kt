package com.mridul.telcootprelay

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import org.json.JSONObject

enum class RelayFailureKind {
    HTTP_STATUS,
    CONNECTION_TIMEOUT,
    RESPONSE_TIMEOUT,
    DNS_FAILURE,
    NETWORK_FAILURE
}

data class ChatSendResult(
    val delivered: Boolean,
    val failureKind: RelayFailureKind? = null,
    val httpStatusCode: Int? = null
)

object ChatSender {
    private const val CONNECT_TIMEOUT_MS = 5000
    private const val READ_TIMEOUT_MS = 8000

    fun sendWithRetry(webhookUrl: String, text: String): ChatSendResult = sendWithRetry(
        webhookUrl = webhookUrl,
        text = text,
        connectionFactory = { URL(it).openConnection() as HttpURLConnection },
        sleep = Thread::sleep
    )

    internal fun sendWithRetry(
        webhookUrl: String,
        text: String,
        connectionFactory: (String) -> HttpURLConnection,
        sleep: (Long) -> Unit
    ): ChatSendResult {
        var delayMs = 0L
        var lastResult = ChatSendResult(false, RelayFailureKind.NETWORK_FAILURE)
        repeat(3) { attempt ->
            if (delayMs > 0) sleep(delayMs)
            lastResult = sendOnce(webhookUrl, text, connectionFactory)
            if (lastResult.delivered) return lastResult
            delayMs = if (attempt == 0) 1000L else 2000L
        }
        return lastResult
    }

    private fun sendOnce(
        webhookUrl: String,
        text: String,
        connectionFactory: (String) -> HttpURLConnection
    ): ChatSendResult {
        var connection: HttpURLConnection? = null
        var waitingForResponse = false
        return try {
            connection = connectionFactory(webhookUrl)
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.useCaches = false

            val json = JSONObject().put("text", text).toString()
            connection.connect()
            connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            waitingForResponse = true
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                ChatSendResult(delivered = true)
            } else {
                ChatSendResult(false, RelayFailureKind.HTTP_STATUS, responseCode)
            }
        } catch (_: UnknownHostException) {
            ChatSendResult(false, RelayFailureKind.DNS_FAILURE)
        } catch (_: SocketTimeoutException) {
            ChatSendResult(
                false,
                if (waitingForResponse) {
                    RelayFailureKind.RESPONSE_TIMEOUT
                } else {
                    RelayFailureKind.CONNECTION_TIMEOUT
                }
            )
        } catch (_: Exception) {
            ChatSendResult(false, RelayFailureKind.NETWORK_FAILURE)
        } finally {
            connection?.disconnect()
        }
    }

}
