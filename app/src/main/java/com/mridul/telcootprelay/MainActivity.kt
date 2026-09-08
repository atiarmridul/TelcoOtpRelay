package com.mridul.telcootprelay

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.method.PasswordTransformationMethod
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mridul.telcootprelay.databinding.ActivityMainBinding

class MainActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences(RelaySettings.PREFERENCES_NAME, MODE_PRIVATE)
    }

    private lateinit var binding: ActivityMainBinding
    private val webhookEdit: EditText get() = binding.webhookEdit
    private val sendersEdit: EditText get() = binding.sendersEdit
    private val relayModeSpinner: Spinner get() = binding.relayModeSpinner
    private val modeDescription: TextView get() = binding.modeDescription
    private val allMessagesPrivacyWarning: TextView get() = binding.allMessagesPrivacyWarning
    private val statusCard: View get() = binding.statusCard
    private val statusText: TextView get() = binding.statusText
    private val webhookVisibilityButton: ImageButton get() = binding.webhookVisibilityButton
    private val saveButton: Button get() = binding.saveButton
    private val lastRelayIcon: TextView get() = binding.lastRelayIcon
    private val lastRelayStatus: TextView get() = binding.lastRelayStatus
    private val lastRelaySender: TextView get() = binding.lastRelaySender
    private val lastRelayTime: TextView get() = binding.lastRelayTime
    private val lastRelayDetail: TextView get() = binding.lastRelayDetail
    private var isWebhookVisible = false
    private val relayStatusPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (LastRelayStore.isStatusKey(key)) {
                runOnUiThread { updateLastRelayStatus() }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configureViews()
        configureContentWidth()
        configureForm()
        observeUnsavedChanges()
        updateModeDescription()
        updateStatus()
        requestNeededPermissions()
    }

    override fun onStart() {
        super.onStart()
        prefs.registerOnSharedPreferenceChangeListener(relayStatusPreferenceListener)
        updateLastRelayStatus()
    }

    override fun onStop() {
        prefs.unregisterOnSharedPreferenceChangeListener(relayStatusPreferenceListener)
        super.onStop()
    }

    private fun configureViews() {
        binding.versionText.text =
            getString(R.string.app_version, installedVersionName())
        saveButton.setOnClickListener { saveSettings() }
        updateSaveButton(false)
        binding.testButton.setOnClickListener { sendTest() }
        webhookVisibilityButton.setOnClickListener {
            setWebhookVisibility(!isWebhookVisible)
        }
        setWebhookVisibility(false)
        val openPrivacy = View.OnClickListener {
            startActivity(Intent(this, AboutPrivacyActivity::class.java))
        }
        binding.privacyCard.setOnClickListener(openPrivacy)
        binding.privacyLink.setOnClickListener(openPrivacy)
        statusCard.setOnClickListener {
            startActivity(Intent(this, RelayHistoryActivity::class.java))
        }
    }

    private fun configureContentWidth() {
        val content = binding.content
        val maxWidth = resources.getDimensionPixelSize(R.dimen.content_max_width)
        content.layoutParams = (content.layoutParams as FrameLayout.LayoutParams).apply {
            width = minOf(resources.displayMetrics.widthPixels, maxWidth)
        }
    }

    private fun configureForm() {
        webhookEdit.setText(prefs.getString(RelaySettings.WEBHOOK_KEY, ""))
        sendersEdit.setText(prefs.getString(RelaySettings.SENDERS_KEY, ""))
        relayModeSpinner.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.relay_mode_options,
            R.layout.spinner_item
        ).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        relayModeSpinner.setSelection(RelayMode.entries.indexOf(loadRelayMode()))
    }

    private fun setWebhookVisibility(visible: Boolean) {
        isWebhookVisible = visible
        webhookEdit.transformationMethod = if (visible) null else PasswordTransformationMethod.getInstance()
        webhookVisibilityButton.setImageResource(
            if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
        )
        webhookVisibilityButton.contentDescription = getString(
            if (visible) R.string.hide_webhook else R.string.show_webhook
        )
        webhookEdit.setSelection(webhookEdit.text.length)
    }

    private fun observeUnsavedChanges() {
        webhookEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                webhookEdit.error = null
                showUnsavedIfNeeded()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
        sendersEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                showUnsavedIfNeeded()
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
        relayModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateModeDescription()
                showUnsavedIfNeeded()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun showUnsavedIfNeeded() {
        val savedWebhook = prefs.getString(RelaySettings.WEBHOOK_KEY, "").orEmpty()
        val savedSenders = prefs.getString(RelaySettings.SENDERS_KEY, "").orEmpty()
        val formState = ConfigurationState.edited(
            currentWebhook = webhookEdit.text.toString(),
            currentSenders = sendersEdit.text.toString(),
            currentMode = selectedRelayMode(),
            savedWebhook = savedWebhook,
            savedSenders = savedSenders,
            savedMode = loadRelayMode()
        )
        updateSaveButton(formState.hasUnsavedChanges)
        renderStatus(formState.status)
    }

    private fun updateModeDescription() {
        val allMessagesSelected = selectedRelayMode() == RelayMode.ALL_MESSAGES
        modeDescription.setText(
            if (allMessagesSelected) {
                R.string.mode_all_description
            } else {
                R.string.mode_otp_description
            }
        )
        allMessagesPrivacyWarning.visibility = if (allMessagesSelected) View.VISIBLE else View.GONE
    }

    private fun saveSettings() {
        val webhook = webhookEdit.text.toString().trim()
        if (!webhook.startsWith("https://chat.googleapis.com/")) {
            webhookEdit.error = getString(R.string.invalid_webhook)
            renderStatus(ConfigurationStatus.INVALID)
            return
        }
        val relayMode = selectedRelayMode()
        val senders = SenderFilter.serialize(sendersEdit.text.toString())
        prefs.edit()
            .putString(RelaySettings.WEBHOOK_KEY, webhook)
            .putString(RelaySettings.MODE_KEY, relayMode.name)
            .putString(RelaySettings.SENDERS_KEY, senders)
            .apply()
        sendersEdit.setText(senders)
        updateSaveButton(false)
        updateStatus()
        statusText.animate().alpha(0.55f).setDuration(90).withEndAction {
            statusText.animate().alpha(1f).setDuration(180).start()
        }.start()
    }

    private fun sendTest() {
        val webhook = webhookEdit.text.toString().trim()
        if (webhook.isBlank()) {
            webhookEdit.error = getString(R.string.enter_webhook)
            renderStatus(ConfigurationStatus.MISSING_WEBHOOK)
            return
        }
        renderStatus(ConfigurationStatus.TESTING)
        Thread {
            val sendResult = ChatSender.sendWithRetry(
                webhook,
                SmsFormatter.format(
                    receivingNumber = "Test Number",
                    sender = "Test Sender",
                    timestampMillis = System.currentTimeMillis(),
                    body = getString(R.string.test_message_body)
                )
            )
            runOnUiThread {
                renderStatus(
                    if (sendResult.delivered) ConfigurationStatus.CONNECTED else ConfigurationStatus.FAILED
                )
            }
        }.start()
    }

    private fun updateStatus() {
        renderStatus(
            ConfigurationState.persisted(prefs.getString(RelaySettings.WEBHOOK_KEY, ""))
        )
    }

    private fun updateLastRelayStatus() {
        val lastRelay = LastRelayStore.load(prefs)
        val visibility = if (lastRelay == null) View.GONE else View.VISIBLE
        statusCard.visibility = visibility
        if (lastRelay == null) return

        val delivered = lastRelay.result == RelayDeliveryResult.DELIVERED
        statusCard.setBackgroundResource(
            if (delivered) R.drawable.bg_status_success else R.drawable.bg_status_error
        )
        lastRelayIcon.text = getString(
            if (delivered) R.string.status_icon_success else R.string.status_icon_attention
        )
        lastRelayIcon.setTextColor(
            ContextCompat.getColor(
                this,
                if (delivered) R.color.success_text else R.color.error_text
            )
        )
        lastRelayStatus.setText(
            if (delivered) R.string.last_relay_delivered else R.string.last_relay_failed
        )
        lastRelayStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (delivered) R.color.success_text else R.color.error_text
            )
        )
        lastRelaySender.text = getString(R.string.last_relay_sender, lastRelay.sender)
        lastRelayTime.text = lastRelay.formatTimestamp()
        lastRelayDetail.visibility = if (delivered) View.GONE else View.VISIBLE
        if (!delivered) {
            lastRelayDetail.text = when (lastRelay.failureKind) {
                RelayFailureKind.HTTP_STATUS -> lastRelay.httpStatusCode?.let {
                    getString(R.string.last_relay_http_failure, it)
                } ?: getString(R.string.last_relay_retries_exhausted)
                RelayFailureKind.CONNECTION_TIMEOUT ->
                    getString(R.string.last_relay_connection_timeout)
                RelayFailureKind.RESPONSE_TIMEOUT ->
                    getString(R.string.last_relay_response_timeout)
                RelayFailureKind.DNS_FAILURE -> getString(R.string.last_relay_dns_failure)
                RelayFailureKind.NETWORK_FAILURE -> getString(R.string.last_relay_network_failure)
                null -> getString(R.string.last_relay_retries_exhausted)
            }
        }
    }

    private fun renderStatus(status: ConfigurationStatus) {
        val titleResource = when (status) {
            ConfigurationStatus.SETUP,
            ConfigurationStatus.MISSING_WEBHOOK -> R.string.status_setup_title
            ConfigurationStatus.ACTIVE -> R.string.status_active_title
            ConfigurationStatus.UNSAVED -> R.string.status_unsaved_title
            ConfigurationStatus.INVALID -> R.string.status_invalid_title
            ConfigurationStatus.TESTING -> R.string.status_testing_title
            ConfigurationStatus.CONNECTED -> R.string.status_test_success_title
            ConfigurationStatus.FAILED -> R.string.status_test_failed_title
        }
        val backgroundResource = when (status) {
            ConfigurationStatus.SETUP,
            ConfigurationStatus.UNSAVED -> R.drawable.bg_status_warning
            ConfigurationStatus.ACTIVE,
            ConfigurationStatus.CONNECTED -> R.drawable.bg_status_success
            ConfigurationStatus.TESTING -> R.drawable.bg_status_info
            ConfigurationStatus.MISSING_WEBHOOK,
            ConfigurationStatus.INVALID,
            ConfigurationStatus.FAILED -> R.drawable.bg_status_error
        }
        val textColorResource = when (status) {
            ConfigurationStatus.SETUP,
            ConfigurationStatus.UNSAVED -> R.color.warning_text
            ConfigurationStatus.ACTIVE,
            ConfigurationStatus.CONNECTED -> R.color.success_text
            ConfigurationStatus.TESTING -> R.color.info_text
            ConfigurationStatus.MISSING_WEBHOOK,
            ConfigurationStatus.INVALID,
            ConfigurationStatus.FAILED -> R.color.error_text
        }
        val textColor = ContextCompat.getColor(this, textColorResource)
        statusText.setText(titleResource)
        statusText.setTextColor(textColor)
        statusText.setBackgroundResource(backgroundResource)
        statusText.setCompoundDrawablesWithIntrinsicBounds(
            when (textColorResource) {
                R.color.success_text -> R.drawable.ic_status_dot_success
                R.color.error_text -> R.drawable.ic_status_dot_error
                R.color.info_text -> R.drawable.ic_status_dot_info
                else -> R.drawable.ic_status_dot_warning
            },
            0,
            0,
            0
        )
    }

    private fun updateSaveButton(hasUnsavedChanges: Boolean) {
        saveButton.isEnabled = hasUnsavedChanges
        saveButton.alpha = if (hasUnsavedChanges) 1f else 0.45f
        saveButton.setText(
            if (hasUnsavedChanges) R.string.save_configuration else R.string.configuration_saved
        )
    }

    private fun selectedRelayMode(): RelayMode = RelayMode.entries.getOrElse(
        relayModeSpinner.selectedItemPosition
    ) { RelayMode.OTP_ONLY }

    private fun loadRelayMode(): RelayMode = RelayMode.fromStored(
        prefs.getString(RelaySettings.MODE_KEY, null)
    )

    private fun requestNeededPermissions() {
        val missing = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_NUMBERS
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle(R.string.required_access_title)
            .setMessage(R.string.required_access_message)
            .setPositiveButton(R.string.allow) { _, _ ->
                requestPermissions(missing.toTypedArray(), 100)
            }
            .setNegativeButton(R.string.not_now, null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun installedVersionName(): String =
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
}
