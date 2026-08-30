package com.mridul.telcootprelay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpParserTest {

    @Test
    fun `accepts keyword followed by four digit code`() {
        assertTrue(OtpParser.isOtpMessage("Your verification code is 4821"))
    }

    @Test
    fun `accepts keyword case insensitively`() {
        assertTrue(OtpParser.isOtpMessage("OTP: 482193"))
    }

    @Test
    fun `accepts standalone six digit code without keyword`() {
        assertTrue(OtpParser.isOtpMessage("Use 482193 to continue"))
    }

    @Test
    fun `rejects message without otp pattern`() {
        assertFalse(OtpParser.isOtpMessage("Your parcel will arrive tomorrow"))
    }

    @Test
    fun `rejects six digits embedded in a longer number`() {
        assertFalse(OtpParser.isOtpMessage("Reference 14821937 received"))
    }

    @Test
    fun `rejects empty message`() {
        assertFalse(OtpParser.isOtpMessage(""))
    }

    @Test
    fun `extracts keyword code before another standalone number`() {
        assertEquals("4821", OtpParser.extract("Code: 4821. Reference: 654321"))
    }

    @Test
    fun `returns null when no numeric candidate exists`() {
        assertNull(OtpParser.extract("No verification value is present"))
    }
}
