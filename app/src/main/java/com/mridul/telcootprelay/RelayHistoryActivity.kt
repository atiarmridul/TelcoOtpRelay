package com.mridul.telcootprelay

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mridul.telcootprelay.databinding.ActivityRelayHistoryBinding

class RelayHistoryActivity : Activity() {
    private lateinit var binding: ActivityRelayHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelayHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.historyBackButton.setOnClickListener { finish() }
        val preferences = getSharedPreferences(RelaySettings.PREFERENCES_NAME, MODE_PRIVATE)
        RelayHistoryStore.migrateLatestIfNeeded(this, preferences)
        binding.historyList.emptyView = binding.historyEmpty
        showHistory()
        binding.clearHistoryButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.clear_history_confirmation_title)
                .setMessage(R.string.clear_history_confirmation_message)
                .setPositiveButton(R.string.clear_history) { _, _ ->
                    RelayHistoryStore.clear(this)
                    LastRelayStore.clear(preferences)
                    showHistory()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun showHistory() {
        val entries = RelayHistoryStore.loadAll(this)
        binding.historyList.adapter = HistoryAdapter(entries)
        binding.clearHistoryButton.isEnabled = entries.isNotEmpty()
        binding.clearHistoryButton.alpha = if (entries.isNotEmpty()) 1f else 0.45f
    }

    private inner class HistoryAdapter(
        private val entries: List<RelayHistoryEntry>
    ) : BaseAdapter() {
        override fun getCount(): Int = entries.size
        override fun getItem(position: Int): RelayHistoryEntry = entries[position]
        override fun getItemId(position: Int): Long = entries[position].id

        override fun getView(position: Int, recycledView: View?, parent: ViewGroup): View {
            val view = recycledView ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_relay_history, parent, false)
            val status = getItem(position).status
            val delivered = status.result == RelayDeliveryResult.DELIVERED
            val color = ContextCompat.getColor(
                this@RelayHistoryActivity,
                if (delivered) R.color.success_text else R.color.error_text
            )

            view.setBackgroundResource(
                if (delivered) R.drawable.bg_status_success else R.drawable.bg_status_error
            )
            view.findViewById<TextView>(R.id.history_item_icon).apply {
                text = getString(
                    if (delivered) R.string.status_icon_success else R.string.status_icon_attention
                )
                setTextColor(color)
            }
            view.findViewById<TextView>(R.id.history_item_status).apply {
                setText(if (delivered) R.string.history_delivered else R.string.history_failed)
                setTextColor(color)
            }
            view.findViewById<TextView>(R.id.history_item_sender).text =
                getString(R.string.last_relay_sender, status.sender)
            view.findViewById<TextView>(R.id.history_item_time).text = status.formatTimestamp()
            view.findViewById<TextView>(R.id.history_item_reason).apply {
                visibility = if (delivered) View.GONE else View.VISIBLE
                if (!delivered) text = failureReason(status)
            }
            return view
        }
    }

    private fun failureReason(status: LastRelayStatus): String = when (status.failureKind) {
        RelayFailureKind.NO_INTERNET -> getString(R.string.last_relay_no_internet)
        RelayFailureKind.DELIVERY_DEADLINE -> getString(R.string.last_relay_delivery_deadline)
        RelayFailureKind.RETRY_EXPIRED -> getString(R.string.last_relay_retry_expired)
        RelayFailureKind.HTTP_STATUS -> status.httpStatusCode?.let {
            getString(R.string.last_relay_http_failure, it)
        } ?: getString(R.string.last_relay_retries_exhausted)
        RelayFailureKind.CONNECTION_TIMEOUT -> getString(R.string.last_relay_connection_timeout)
        RelayFailureKind.RESPONSE_TIMEOUT -> getString(R.string.last_relay_response_timeout)
        RelayFailureKind.DNS_FAILURE -> getString(R.string.last_relay_dns_failure)
        RelayFailureKind.NETWORK_FAILURE -> getString(R.string.last_relay_network_failure)
        null -> getString(R.string.last_relay_retries_exhausted)
    }
}
