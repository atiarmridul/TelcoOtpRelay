package com.mridul.telcootprelay

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class SmsReceiver : BroadcastReceiver() {

    companion object {
        // One shared worker; no thread is created for each incoming SMS.
        private val executor = Executors.newSingleThreadExecutor()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pending = goAsync()
        val appContext = context.applicationContext

        executor.execute {
            try {
                val prefs = appContext.getSharedPreferences(
                    RelaySettings.PREFERENCES_NAME,
                    Context.MODE_PRIVATE
                )
                val webhook = prefs.getString(RelaySettings.WEBHOOK_KEY, "")
                    ?.trim()
                    .orEmpty()
                if (webhook.isBlank()) return@execute

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isEmpty()) return@execute

                // Reconstruct the complete multipart SMS exactly once.
                val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
                if (body.isBlank()) return@execute

                val sender = messages.asSequence()
                    .mapNotNull { it.originatingAddress?.trim()?.takeIf(String::isNotEmpty) }
                    .firstOrNull()
                    ?: "Unknown"

                if (!SenderFilter.allows(
                        sender,
                        prefs.getString(RelaySettings.SENDERS_KEY, null)
                    )
                ) return@execute

                val relayMode = RelayMode.fromStored(
                    prefs.getString(RelaySettings.MODE_KEY, null)
                )
                if (!relayMode.shouldRelay(body)) return@execute

                val timestamp = messages.minOfOrNull { it.timestampMillis }
                    ?: System.currentTimeMillis()

                val subId = intent.getIntExtra(
                    SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID
                ).takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }

                val receivingNumber = SimNumberResolver.resolve(appContext, subId)
                val chatMessage = SmsFormatter.format(
                    receivingNumber = receivingNumber,
                    sender = sender,
                    timestampMillis = timestamp,
                    body = body
                )

                val result = if (ChatSender.sendWithRetry(webhook, chatMessage)) {
                    RelayDeliveryResult.DELIVERED
                } else {
                    RelayDeliveryResult.FAILED
                }
                LastRelayStore.save(
                    preferences = prefs,
                    result = result,
                    sender = sender,
                    timestampMillis = timestamp
                )
            } finally {
                pending.finish()
            }
        }
    }
}

object SimNumberResolver {
    fun resolve(context: Context, subscriptionId: Int?): String {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED
        ) return "Unknown"

        if (subscriptionId != null &&
            subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) {
            try {
                val number = context
                    .getSystemService(SubscriptionManager::class.java)
                    .getPhoneNumber(subscriptionId)
                    .trim()
                if (number.isNotEmpty()) return number
            } catch (_: Exception) { }
        }

        return try {
            val tm = context.getSystemService(android.telephony.TelephonyManager::class.java)
            val scoped = if (
                subscriptionId != null &&
                subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
                android.os.Build.VERSION.SDK_INT >= 24
            ) tm.createForSubscriptionId(subscriptionId) else tm
            scoped.line1Number?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
