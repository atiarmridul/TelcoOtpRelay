package com.mridul.telcootprelay

import java.time.ZoneId
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
    fun `restores privacy safe failure diagnostics`() {
        val status = LastRelayStatus.fromStored(
            result = "FAILED",
            sender = "TEST-SENDER",
            timestampMillis = 0L,
            failureKind = "HTTP_STATUS",
            httpStatusCode = 503
        )

        assertEquals(RelayFailureKind.HTTP_STATUS, status?.failureKind)
        assertEquals(503, status?.httpStatusCode)
    }

    @Test
    fun `restores no internet failure diagnostic`() {
        val status = LastRelayStatus.fromStored(
            result = "FAILED",
            sender = "TEST-SENDER",
            timestampMillis = 0L,
            failureKind = "NO_INTERNET"
        )

        assertEquals(RelayFailureKind.NO_INTERNET, status?.failureKind)
    }

    @Test
    fun `restores receiver deadline failure diagnostic`() {
        val status = LastRelayStatus.fromStored(
            result = "FAILED",
            sender = "TEST-SENDER",
            timestampMillis = 0L,
            failureKind = "DELIVERY_DEADLINE"
        )

        assertEquals(RelayFailureKind.DELIVERY_DEADLINE, status?.failureKind)
    }

    @Test
    fun `restores expired retry diagnostic`() {
        val status = LastRelayStatus.fromStored(
            result = "FAILED",
            sender = "TEST-SENDER",
            timestampMillis = 0L,
            failureKind = "RETRY_EXPIRED"
        )

        assertEquals(RelayFailureKind.RETRY_EXPIRED, status?.failureKind)
    }

    @Test
    fun `ignores unknown failure diagnostics for backward compatibility`() {
        val status = LastRelayStatus.fromStored(
            result = "FAILED",
            sender = "TEST-SENDER",
            timestampMillis = 0L,
            failureKind = "UNKNOWN_FAILURE"
        )

        assertEquals(null, status?.failureKind)
    }

    @Test
    fun `rejects unknown or incomplete stored status`() {
        assertNull(LastRelayStatus.fromStored("UNKNOWN", "TEST-SENDER", 0L))
        assertNull(LastRelayStatus.fromStored("DELIVERED", "TEST-SENDER", -1L))
    }

    @Test
    fun `formats the relay timestamp for display`() {
        val status = LastRelayStatus(RelayDeliveryResult.DELIVERED, "TEST-SENDER", 0L)

        assertEquals(
            "01 Jan 1970, 12:00 AM",
            status.formatTimestamp(Locale.US, ZoneId.of("UTC"))
        )
    }
}
