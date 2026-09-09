package com.mridul.telcootprelay

data class IncomingSms(
    val body: String,
    val sender: String,
    val timestampMillis: Long,
    val subscriptionId: Int?
)

data class RelayConfiguration(
    val webhook: String,
    val mode: RelayMode,
    val allowedSenders: String?,
    val receivingNumber: String? = null
)

data class RelayRequest(
    val webhook: String,
    val message: String,
    val sender: String,
    val timestampMillis: Long
)

object RelayProcessor {
    fun prepare(
        configuration: RelayConfiguration,
        sms: IncomingSms,
        receivingNumber: () -> String
    ): RelayRequest? {
        if (configuration.webhook.isBlank()) return null
        if (!SenderFilter.allows(sms.sender, configuration.allowedSenders)) return null
        if (!configuration.mode.shouldRelay(sms.body)) return null

        return RelayRequest(
            webhook = configuration.webhook,
            message = SmsFormatter.format(
                receivingNumber = configuration.receivingNumber
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?: receivingNumber(),
                sender = sms.sender,
                timestampMillis = sms.timestampMillis,
                body = sms.body
            ),
            sender = sms.sender,
            timestampMillis = sms.timestampMillis
        )
    }
}
