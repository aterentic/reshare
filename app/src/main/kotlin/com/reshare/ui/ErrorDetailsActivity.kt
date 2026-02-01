package com.reshare.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.reshare.notification.ProgressNotifier

class ErrorDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(ProgressNotifier.EXTRA_ERROR_TITLE) ?: "Error"
        val message = intent.getStringExtra(ProgressNotifier.EXTRA_ERROR_MESSAGE) ?: "Unknown error"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
