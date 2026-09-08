package com.mridul.telcootprelay

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSenderTest {
    @Test
    fun `successful response is sent once without delay`() {
        val connections = mutableListOf<FakeConnection>()
        val delays = mutableListOf<Long>()

        val result = ChatSender.sendWithRetry(
            webhookUrl = "https://chat.googleapis.com/example",
            text = "Test message",
            connectionFactory = {
                FakeConnection(204).also(connections::add)
            },
            sleep = delays::add
        )

        assertTrue(result.delivered)
        assertEquals(1, connections.size)
        assertEquals(emptyList<Long>(), delays)
        assertTrue(connections.single().disconnected)
        assertEquals("POST", connections.single().requestMethod)
        assertEquals(5000, connections.single().connectTimeout)
        assertEquals(8000, connections.single().readTimeout)
    }

    @Test
    fun `failed responses use the existing bounded retry schedule`() {
        var attempts = 0
        val delays = mutableListOf<Long>()

        val result = ChatSender.sendWithRetry(
            webhookUrl = "https://chat.googleapis.com/example",
            text = "Test message",
            connectionFactory = {
                attempts += 1
                FakeConnection(500)
            },
            sleep = delays::add
        )

        assertFalse(result.delivered)
        assertEquals(RelayFailureKind.HTTP_STATUS, result.failureKind)
        assertEquals(500, result.httpStatusCode)
        assertEquals(3, attempts)
        assertEquals(listOf(1000L, 2000L), delays)
    }

    @Test
    fun `connection failures are retried and reported as failure`() {
        var attempts = 0

        val result = ChatSender.sendWithRetry(
            webhookUrl = "https://chat.googleapis.com/example",
            text = "Test message",
            connectionFactory = {
                attempts += 1
                throw IllegalStateException("synthetic failure")
            },
            sleep = {}
        )

        assertFalse(result.delivered)
        assertEquals(RelayFailureKind.NETWORK_FAILURE, result.failureKind)
        assertEquals(3, attempts)
    }

    @Test
    fun `connection timeout is reported without exception details`() {
        val result = ChatSender.sendWithRetry(
            webhookUrl = "https://chat.googleapis.com/example",
            text = "Test message",
            connectionFactory = {
                FakeConnection(200, connectFailure = SocketTimeoutException("sensitive detail"))
            },
            sleep = {}
        )

        assertEquals(RelayFailureKind.CONNECTION_TIMEOUT, result.failureKind)
        assertEquals(null, result.httpStatusCode)
    }

    @Test
    fun `response timeout is distinguished from connection timeout`() {
        val result = ChatSender.sendWithRetry(
            webhookUrl = "https://chat.googleapis.com/example",
            text = "Test message",
            connectionFactory = {
                FakeConnection(200, responseFailure = SocketTimeoutException("sensitive detail"))
            },
            sleep = {}
        )

        assertEquals(RelayFailureKind.RESPONSE_TIMEOUT, result.failureKind)
    }

    @Test
    fun `DNS failure is reported without host or exception details`() {
        val result = ChatSender.sendWithRetry(
            webhookUrl = "https://chat.googleapis.com/example",
            text = "Test message",
            connectionFactory = {
                FakeConnection(200, connectFailure = UnknownHostException("sensitive detail"))
            },
            sleep = {}
        )

        assertEquals(RelayFailureKind.DNS_FAILURE, result.failureKind)
    }

    @Test
    fun `message is encoded by a standard JSON writer without content loss`() {
        val connection = FakeConnection(200)
        val message = "Quote \" slash \\ line\n tab\t backspace\b form-feed\u000C বাংলা"

        val result = ChatSender.sendWithRetry(
            webhookUrl = "https://chat.googleapis.com/example",
            text = message,
            connectionFactory = { connection },
            sleep = {}
        )

        assertTrue(result.delivered)
        assertEquals(message, JSONObject(connection.requestBodyText()).getString("text"))
    }

    private class FakeConnection(
        private val statusCode: Int,
        private val connectFailure: Exception? = null,
        private val responseFailure: Exception? = null
    ) : HttpURLConnection(URL("https://chat.googleapis.com/example")) {
        private val requestBody = ByteArrayOutputStream()
        var disconnected = false
            private set

        override fun connect() {
            connectFailure?.let { throw it }
        }

        override fun disconnect() {
            disconnected = true
        }

        override fun usingProxy(): Boolean = false

        override fun getOutputStream(): ByteArrayOutputStream = requestBody

        override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))

        override fun getResponseCode(): Int {
            responseFailure?.let { throw it }
            return statusCode
        }

        fun requestBodyText(): String = requestBody.toString(Charsets.UTF_8.name())
    }
}
