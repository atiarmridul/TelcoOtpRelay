package com.mridul.telcootprelay

import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SmsFormatterTest {
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
    fun `formats all message fields and preserves multiline body`() {
        val formatted = SmsFormatter.format(
            receivingNumber = "01XXXXXXXXX",
            sender = "TEST-SENDER",
            timestampMillis = 0L,
            body = "Your code is 482193\nDo not share it."
        )

        assertEquals(
            "Receiving number: 01XXXXXXXXX\n" +
                "Sender: TEST-SENDER\n" +
                "Timestamp: 01 Jan 1970, 12:00:00 AM\n" +
                "Body: Your code is 482193\nDo not share it.",
            formatted
        )
    }

    @Test
    fun `uses unknown for blank phone number and sender`() {
        val formatted = SmsFormatter.format(
            receivingNumber = "",
            sender = "",
            timestampMillis = 0L,
            body = "Code 482193"
        )

        assertEquals(
            "Receiving number: Unknown\n" +
                "Sender: Unknown\n" +
                "Timestamp: 01 Jan 1970, 12:00:00 AM\n" +
                "Body: Code 482193",
            formatted
        )
    }
}
