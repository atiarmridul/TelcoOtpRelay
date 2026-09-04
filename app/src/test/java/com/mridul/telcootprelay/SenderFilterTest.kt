package com.mridul.telcootprelay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderFilterTest {
    @Test
    fun `blank configuration allows every sender`() {
        assertTrue(SenderFilter.allows("+8801712345678", null))
        assertTrue(SenderFilter.allows("BANK-OTP", "  "))
    }

    @Test
    fun `configured list accepts only matching senders`() {
        val configured = "+8801712345678, BANK-OTP"

        assertTrue(SenderFilter.allows("+8801712345678", configured))
        assertTrue(SenderFilter.allows("bank-otp", configured))
        assertFalse(SenderFilter.allows("OTHER", configured))
    }

    @Test
    fun `phone formatting characters do not affect matching`() {
        assertTrue(SenderFilter.allows("+880 1712-345678", "+880 (1712) 345678"))
    }

    @Test
    fun `unknown sender is rejected when a sender is configured`() {
        assertFalse(SenderFilter.allows(null, "+8801712345678"))
    }

    @Test
    fun `serialization accepts commas semicolons and new lines`() {
        assertEquals("ONE, TWO, THREE", SenderFilter.serialize(" ONE;TWO\nTHREE, one "))
    }
}
