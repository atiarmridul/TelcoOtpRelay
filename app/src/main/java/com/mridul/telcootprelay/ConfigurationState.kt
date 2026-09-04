package com.mridul.telcootprelay

enum class ConfigurationStatus {
    SETUP,
    MISSING_WEBHOOK,
    ACTIVE,
    UNSAVED,
    INVALID,
    TESTING,
    CONNECTED,
    FAILED
}

data class ConfigurationFormState(
    val status: ConfigurationStatus,
    val hasUnsavedChanges: Boolean
)

object ConfigurationState {
    fun persisted(webhook: String?): ConfigurationStatus =
        if (webhook.isNullOrBlank()) ConfigurationStatus.SETUP else ConfigurationStatus.ACTIVE

    fun edited(
        currentWebhook: String,
        currentSenders: String?,
        currentMode: RelayMode,
        savedWebhook: String?,
        savedSenders: String?,
        savedMode: RelayMode
    ): ConfigurationFormState {
        val changed = currentWebhook.trim() != savedWebhook.orEmpty() ||
            SenderFilter.serialize(currentSenders) != savedSenders.orEmpty() ||
            currentMode != savedMode

        return ConfigurationFormState(
            status = if (changed) ConfigurationStatus.UNSAVED else persisted(savedWebhook),
            hasUnsavedChanges = changed
        )
    }
}
