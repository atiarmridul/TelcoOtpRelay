package com.mridul.telcootprelay

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import org.json.JSONObject

enum class RelayFailureKind {
    NO_INTERNET,
    DELIVERY_DEADLINE,
    RETRY_EXPIRED,
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
    private const val DELIVERY_BUDGET_MS = 8000L
    private const val MAX_STAGE_TIMEOUT_MS = 2000

    fun sendWithRetry(webhookUrl: String, text: String): ChatSendResult = sendWithRetry(
        webhookUrl = webhookUrl,
        text = text,
        connectionFactory = { URL(it).openConnection() as HttpURLConnection },
        sleep = Thread::sleep,
        elapsedRealtime = { System.nanoTime() / 1_000_000L }
    )

    internal fun sendWithRetry(
        webhookUrl: String,
        text: String,
        connectionFactory: (String) -> HttpURLConnection,
        sleep: (Long) -> Unit,
        elapsedRealtime: () -> Long = { System.nanoTime() / 1_000_000L }
    ): ChatSendResult {
        val deadline = elapsedRealtime() + DELIVERY_BUDGET_MS
        var delayMs = 0L
        var lastResult = ChatSendResult(false, RelayFailureKind.NETWORK_FAILURE)
        repeat(3) { attempt ->
            val remainingBeforeDelay = deadline - elapsedRealtime()
            if (remainingBeforeDelay <= delayMs) return lastResult
            if (delayMs > 0) sleep(delayMs)
            val remaining = deadline - elapsedRealtime()
            if (remaining <= 0) return lastResult
            lastResult = sendOnce(webhookUrl, text, connectionFactory, remaining)
            if (lastResult.delivered) return lastResult
            delayMs = if (attempt == 0) 250L else 500L
        }
        return lastResult
    }

    private fun sendOnce(
        webhookUrl: String,
        text: String,
        connectionFactory: (String) -> HttpURLConnection,
        remainingBudgetMs: Long
    ): ChatSendResult {
        var connection: HttpURLConnection? = null
        var waitingForResponse = false
        return try {
            connection = connectionFactory(webhookUrl)
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            // A connection and response can consume their timeouts serially. Splitting the
            // remaining receiver budget keeps the complete attempt inside the hard deadline.
            val stageTimeout = minOf(
                MAX_STAGE_TIMEOUT_MS,
                (remainingBudgetMs / 2).coerceAtLeast(1L).toInt()
            )
            connection.connectTimeout = stageTimeout
            connection.readTimeout = stageTimeout
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
