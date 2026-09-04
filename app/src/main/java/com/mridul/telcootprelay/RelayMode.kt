package com.mridul.telcootprelay

enum class RelayMode {
    OTP_ONLY,
    ALL_MESSAGES;

    fun shouldRelay(message: String): Boolean =
        message.isNotBlank() && (this == ALL_MESSAGES || OtpParser.isOtpMessage(message))

    companion object {
        fun fromStored(value: String?): RelayMode =
            entries.firstOrNull { it.name == value } ?: OTP_ONLY
    }
}

object RelaySettings {
    const val PREFERENCES_NAME = "relay"
    const val WEBHOOK_KEY = "webhook"
    const val MODE_KEY = "relay_mode"
    const val SENDERS_KEY = "allowed_senders"
}
