package com.mridul.telcootprelay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationStateTest {
    @Test
    fun `missing persisted webhook requires setup`() {
        assertEquals(ConfigurationStatus.SETUP, ConfigurationState.persisted(null))
        assertEquals(ConfigurationStatus.SETUP, ConfigurationState.persisted("  "))
    }

    @Test
    fun `persisted webhook is active`() {
        assertEquals(
            ConfigurationStatus.ACTIVE,
            ConfigurationState.persisted("https://chat.googleapis.com/example")
        )
    }

    @Test
    fun `matching form remains active and saved`() {
        val state = ConfigurationState.edited(
            currentWebhook = " https://chat.googleapis.com/example ",
            currentSenders = "BANK-OTP; +880 1712-345678",
            currentMode = RelayMode.OTP_ONLY,
            savedWebhook = "https://chat.googleapis.com/example",
            savedSenders = "BANK-OTP, +880 1712-345678",
            savedMode = RelayMode.OTP_ONLY
        )

        assertEquals(ConfigurationStatus.ACTIVE, state.status)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `any changed field produces unsaved state`() {
        val base = ConfigurationState.edited(
            currentWebhook = "https://chat.googleapis.com/example",
            currentSenders = "BANK-OTP",
            currentMode = RelayMode.ALL_MESSAGES,
            savedWebhook = "https://chat.googleapis.com/example",
            savedSenders = "BANK-OTP",
            savedMode = RelayMode.OTP_ONLY
        )

        assertEquals(ConfigurationStatus.UNSAVED, base.status)
        assertTrue(base.hasUnsavedChanges)
    }
}
