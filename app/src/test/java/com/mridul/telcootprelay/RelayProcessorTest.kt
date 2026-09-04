package com.mridul.telcootprelay

import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RelayProcessorTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun useUtcTimeZone() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `accepted message becomes the existing Google Chat request`() {
        val request = RelayProcessor.prepare(
            configuration = RelayConfiguration(
                webhook = "https://chat.googleapis.com/example",
                mode = RelayMode.OTP_ONLY,
                allowedSenders = "TEST-SENDER"
            ),
            sms = IncomingSms(
                body = "Your code is 482193",
                sender = "TEST-SENDER",
                timestampMillis = 0L,
                subscriptionId = 1
            ),
            receivingNumber = { "01XXXXXXXXX" }
        )

        assertEquals("https://chat.googleapis.com/example", request?.webhook)
        assertEquals("TEST-SENDER", request?.sender)
        assertEquals(0L, request?.timestampMillis)
        assertEquals(
            "Receiving number: 01XXXXXXXXX\n" +
                "Sender: TEST-SENDER\n" +
                "Timestamp: 01 Jan 1970, 12:00:00 AM\n" +
                "Body: Your code is 482193",
            request?.message
        )
    }

    @Test
    fun `rejected messages do not resolve the receiving number`() {
        var resolutionCount = 0
        val request = RelayProcessor.prepare(
            configuration = RelayConfiguration(
                webhook = "https://chat.googleapis.com/example",
                mode = RelayMode.OTP_ONLY,
                allowedSenders = "EXPECTED"
            ),
            sms = IncomingSms(
                body = "Your code is 482193",
                sender = "OTHER",
                timestampMillis = 0L,
                subscriptionId = null
            ),
            receivingNumber = {
                resolutionCount += 1
                "Unknown"
            }
        )

        assertNull(request)
        assertEquals(0, resolutionCount)
    }

    @Test
    fun `blank webhook and rejected relay mode produce no request`() {
        val sms = IncomingSms("Ordinary message", "TEST-SENDER", 0L, null)

        assertNull(
            RelayProcessor.prepare(
                RelayConfiguration("", RelayMode.ALL_MESSAGES, null),
                sms
            ) { "Unknown" }
        )
        assertNull(
            RelayProcessor.prepare(
                RelayConfiguration(
                    "https://chat.googleapis.com/example",
                    RelayMode.OTP_ONLY,
                    null
                ),
                sms
            ) { "Unknown" }
        )
    }
}
