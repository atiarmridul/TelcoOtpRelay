package com.mridul.telcootprelay

import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager

object SmsBroadcastParser {
    fun parse(intent: Intent): IncomingSms? {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return null

        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        if (body.isBlank()) return null

        val sender = messages.asSequence()
            .mapNotNull { it.originatingAddress?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull()
            ?: "Unknown"
        val timestamp = messages.minOfOrNull { it.timestampMillis }
            ?: System.currentTimeMillis()
        val subscriptionId = intent.getIntExtra(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        ).takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }

        return IncomingSms(
            body = body,
            sender = sender,
            timestampMillis = timestamp,
            subscriptionId = subscriptionId
        )
    }
}
