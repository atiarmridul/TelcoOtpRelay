package com.mridul.telcootprelay

import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LastRelayStatusTest {
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
    fun `restores a delivered relay without message content`() {
        val status = LastRelayStatus.fromStored(
            result = "DELIVERED",
            sender = "TEST-SENDER",
            timestampMillis = 0L
        )

        assertEquals(
            LastRelayStatus(RelayDeliveryResult.DELIVERED, "TEST-SENDER", 0L),
            status
        )
    }

    @Test
    fun `uses unknown for a blank stored sender`() {
        val status = LastRelayStatus.fromStored("FAILED", "  ", 0L)

        assertEquals("Unknown", status?.sender)
    }

    @Test
    fun `rejects unknown or incomplete stored status`() {
        assertNull(LastRelayStatus.fromStored("UNKNOWN", "TEST-SENDER", 0L))
        assertNull(LastRelayStatus.fromStored("DELIVERED", "TEST-SENDER", -1L))
    }

    @Test
    fun `formats the relay timestamp for display`() {
        val status = LastRelayStatus(RelayDeliveryResult.DELIVERED, "TEST-SENDER", 0L)

        assertEquals("01 Jan 1970, 12:00 AM", status.formatTimestamp(Locale.US))
    }
}
