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
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private val prefs by lazy {
        getSharedPreferences(RelaySettings.PREFERENCES_NAME, MODE_PRIVATE)
    }

    private lateinit var webhookEdit: EditText
    private lateinit var sendersEdit: EditText
    private lateinit var relayModeSpinner: Spinner
    private lateinit var modeDescription: TextView
    private lateinit var allMessagesPrivacyWarning: TextView
    private lateinit var statusCard: View
    private lateinit var statusText: TextView
    private lateinit var webhookVisibilityButton: ImageButton
    private lateinit var saveButton: Button
    private lateinit var lastRelayIcon: TextView
    private lateinit var lastRelayStatus: TextView
    private lateinit var lastRelaySender: TextView
    private lateinit var lastRelayTime: TextView
    private lateinit var lastRelayDetail: TextView
    private var isWebhookVisible = false
    private val relayStatusPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (LastRelayStore.isStatusKey(key)) {
                runOnUiThread { updateLastRelayStatus() }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
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

    private fun bindViews() {
        webhookEdit = findViewById(R.id.webhook_edit)
        sendersEdit = findViewById(R.id.senders_edit)
        webhookVisibilityButton = findViewById(R.id.webhook_visibility_button)
        relayModeSpinner = findViewById(R.id.relay_mode_spinner)
        modeDescription = findViewById(R.id.mode_description)
        allMessagesPrivacyWarning = findViewById(R.id.all_messages_privacy_warning)
        statusCard = findViewById(R.id.status_card)
        statusText = findViewById(R.id.status_text)
        lastRelayIcon = findViewById(R.id.last_relay_icon)
        lastRelayStatus = findViewById(R.id.last_relay_status)
        lastRelaySender = findViewById(R.id.last_relay_sender)
        lastRelayTime = findViewById(R.id.last_relay_time)
        lastRelayDetail = findViewById(R.id.last_relay_detail)
        findViewById<TextView>(R.id.version_text).text =
            getString(R.string.app_version, installedVersionName())
        saveButton = findViewById(R.id.save_button)
        saveButton.setOnClickListener { saveSettings() }
        updateSaveButton(false)
        findViewById<Button>(R.id.test_button).setOnClickListener { sendTest() }
        webhookVisibilityButton.setOnClickListener {
            setWebhookVisibility(!isWebhookVisible)
        }
        setWebhookVisibility(false)
        val openPrivacy = View.OnClickListener {
            startActivity(Intent(this, AboutPrivacyActivity::class.java))
        }
        findViewById<View>(R.id.privacy_card).setOnClickListener(openPrivacy)
        findViewById<TextView>(R.id.privacy_link).setOnClickListener(openPrivacy)
    }

    private fun configureContentWidth() {
        val content = findViewById<View>(R.id.content)
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
        val hasUnsavedChanges =
            webhookEdit.text.toString().trim() != savedWebhook ||
                SenderFilter.serialize(sendersEdit.text.toString()) != savedSenders ||
                selectedRelayMode() != loadRelayMode()
        updateSaveButton(hasUnsavedChanges)
        if (hasUnsavedChanges) {
            showStatus(
                R.string.status_unsaved_title,
                R.drawable.bg_status_warning,
                R.color.warning_text
            )
        } else {
            updateStatus()
        }
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
            showStatus(
                R.string.status_invalid_title,
                R.drawable.bg_status_error,
                R.color.error_text
            )
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
            showStatus(
                R.string.status_setup_title,
                R.drawable.bg_status_error,
                R.color.error_text
            )
            return
        }
        showStatus(
            R.string.status_testing_title,
            R.drawable.bg_status_info,
            R.color.info_text
        )
        Thread {
            val ok = ChatSender.sendWithRetry(
                webhook,
                SmsFormatter.format(
                    receivingNumber = "Test Number",
                    sender = "Test Sender",
                    timestampMillis = System.currentTimeMillis(),
                    body = getString(R.string.test_message_body)
                )
            )
            runOnUiThread {
                showStatus(
                    if (ok) R.string.status_test_success_title else R.string.status_test_failed_title,
                    if (ok) R.drawable.bg_status_success else R.drawable.bg_status_error,
                    if (ok) R.color.success_text else R.color.error_text
                )
            }
        }.start()
    }

    private fun updateStatus() {
        if (prefs.getString(RelaySettings.WEBHOOK_KEY, "").isNullOrBlank()) {
            showStatus(
                R.string.status_setup_title,
                R.drawable.bg_status_warning,
                R.color.warning_text
            )
            return
        }
        showStatus(
            R.string.status_active_title,
            R.drawable.bg_status_success,
            R.color.success_text
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
    }

    private fun showStatus(
        @StringRes titleResource: Int,
        @DrawableRes backgroundResource: Int,
        @ColorRes textColorResource: Int
    ) {
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
