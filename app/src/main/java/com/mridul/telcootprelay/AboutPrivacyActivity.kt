package com.mridul.telcootprelay

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import com.mridul.telcootprelay.databinding.ActivityAboutPrivacyBinding

class AboutPrivacyActivity : Activity() {
    private lateinit var binding: ActivityAboutPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val content = binding.aboutPrivacyContent
        val maxWidth = resources.getDimensionPixelSize(R.dimen.content_max_width)
        content.layoutParams = (content.layoutParams as FrameLayout.LayoutParams).apply {
            width = minOf(resources.displayMetrics.widthPixels, maxWidth)
        }

        binding.backButton.setOnClickListener { finish() }
    }
}
