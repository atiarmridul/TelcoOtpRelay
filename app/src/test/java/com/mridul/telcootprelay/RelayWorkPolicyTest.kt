package com.mridul.telcootprelay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayWorkPolicyTest {
    @Test
    fun `pending relay expires after twenty four hours`() {
        val day = 24L * 60L * 60L * 1000L

        assertFalse(RelayWorkPolicy.isExpired(1_000L, 1_000L + day - 1L))
        assertTrue(RelayWorkPolicy.isExpired(1_000L, 1_000L + day))
    }

    @Test
    fun `transient failures are retried`() {
        assertTrue(
            RelayWorkPolicy.shouldRetry(
                ChatSendResult(false, RelayFailureKind.DNS_FAILURE)
            )
        )
        assertTrue(
            RelayWorkPolicy.shouldRetry(
                ChatSendResult(false, RelayFailureKind.HTTP_STATUS, 429)
            )
        )
        assertTrue(
            RelayWorkPolicy.shouldRetry(
                ChatSendResult(false, RelayFailureKind.HTTP_STATUS, 503)
            )
        )
    }

    @Test
    fun `permanent HTTP failures are not retried`() {
        assertFalse(
            RelayWorkPolicy.shouldRetry(
                ChatSendResult(false, RelayFailureKind.HTTP_STATUS, 400)
            )
        )
        assertFalse(
            RelayWorkPolicy.shouldRetry(
                ChatSendResult(false, RelayFailureKind.HTTP_STATUS, 600)
            )
        )
        assertFalse(RelayWorkPolicy.shouldRetry(ChatSendResult(delivered = true)))
    }
}
