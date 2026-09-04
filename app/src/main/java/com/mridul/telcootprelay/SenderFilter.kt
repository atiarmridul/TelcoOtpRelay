package com.mridul.telcootprelay

import java.util.Locale

object SenderFilter {
    private val separator = Regex("[,;\\n]")
    private val formattingCharacters = Regex("[\\s()\\-.]")

    fun parse(value: String?): List<String> = value.orEmpty()
        .split(separator)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(::normalized)

    fun serialize(value: String?): String = parse(value).joinToString(", ")

    fun allows(sender: String?, storedSenders: String?): Boolean {
        val allowedSenders = parse(storedSenders)
        if (allowedSenders.isEmpty()) return true

        val normalizedSender = normalized(sender.orEmpty())
        return normalizedSender.isNotEmpty() && allowedSenders.any {
            normalized(it) == normalizedSender
        }
    }

    private fun normalized(value: String): String = value
        .trim()
        .replace(formattingCharacters, "")
        .lowercase(Locale.ROOT)
}
