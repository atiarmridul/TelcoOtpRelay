package com.mridul.telcootprelay

object OtpParser {
    private val keywordOtp = Regex(
        "(?i)(?:otp|one[- ]?time password|verification|verify|authentication|login|code|passcode|pin)[^0-9]{0,80}(\\d{4,8})(?!\\d)"
    )
    private val sixDigit = Regex("(?<!\\d)\\d{6}(?!\\d)")
    private val fourToEightDigit = Regex("(?<!\\d)\\d{4,8}(?!\\d)")

    fun isOtpMessage(message: String): Boolean =
        keywordOtp.containsMatchIn(message) || sixDigit.containsMatchIn(message)

    fun extract(message: String): String? =
        keywordOtp.find(message)?.groupValues?.getOrNull(1)
            ?: sixDigit.find(message)?.value
            ?: fourToEightDigit.find(message)?.value
}
