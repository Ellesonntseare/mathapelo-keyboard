package com.mathapelo.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * In Gallery/Photos select a screenshot > Share > Save to Mathapelo.
 * Saved images appear in the keyboard's Copied panel, even on phones
 * that save screenshots to Photos rather than the system clipboard.
 */
class ScreenshotReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        } else null
        val saved = uri != null && ClipboardStore.saveImage(this, uri, Prefs(this))
        Toast.makeText(this, if (saved) "Saved to Mathapelo clipboard" else "Couldn't import this screenshot", Toast.LENGTH_LONG).show()
        finish()
    }
}
