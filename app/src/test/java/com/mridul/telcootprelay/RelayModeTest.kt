package com.mridul.telcootprelay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayModeTest {

    @Test
    fun `missing stored mode defaults to otp only`() {
        assertEquals(RelayMode.OTP_ONLY, RelayMode.fromStored(null))
    }

    @Test
    fun `unknown stored mode defaults to otp only`() {
        assertEquals(RelayMode.OTP_ONLY, RelayMode.fromStored("UNKNOWN"))
    }

    @Test
    fun `stored all messages mode is restored`() {
        assertEquals(RelayMode.ALL_MESSAGES, RelayMode.fromStored("ALL_MESSAGES"))
    }

    @Test
    fun `otp only mode rejects ordinary message`() {
        assertFalse(RelayMode.OTP_ONLY.shouldRelay("Your parcel arrives tomorrow"))
    }

    @Test
    fun `otp only mode accepts otp message`() {
        assertTrue(RelayMode.OTP_ONLY.shouldRelay("Your code is 482193"))
    }

    @Test
    fun `all messages mode accepts ordinary message`() {
        assertTrue(RelayMode.ALL_MESSAGES.shouldRelay("Your parcel arrives tomorrow"))
    }

    @Test
    fun `all messages mode rejects blank body`() {
        assertFalse(RelayMode.ALL_MESSAGES.shouldRelay("   "))
    }
}
