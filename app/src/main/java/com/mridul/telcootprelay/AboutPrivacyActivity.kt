package com.mridul.telcootprelay

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout

class AboutPrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_privacy)

        val content = findViewById<View>(R.id.about_privacy_content)
        val maxWidth = resources.getDimensionPixelSize(R.dimen.content_max_width)
        content.layoutParams = (content.layoutParams as FrameLayout.LayoutParams).apply {
            width = minOf(resources.displayMetrics.widthPixels, maxWidth)
        }

        findViewById<View>(R.id.back_button).setOnClickListener { finish() }
    }
}
