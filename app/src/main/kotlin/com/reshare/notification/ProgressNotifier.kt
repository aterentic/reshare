package com.reshare.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.reshare.R
import com.reshare.converter.ConversionError
import com.reshare.ui.ErrorDetailsActivity

class ProgressNotifier(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createProgressChannel()
        createErrorChannel()
    }

    private fun createProgressChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "Conversion Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows document conversion progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createErrorChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ERROR_CHANNEL_ID,
                "Conversion Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows conversion error notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgress(message: String = "Converting document...") {
        val notification = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setContentTitle("ReShare")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(PROGRESS_NOTIFICATION_ID, notification)
    }

    fun showBatchProgress(current: Int, total: Int) {
        val message = context.getString(R.string.batch_converting, current, total)
        val notification = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setContentTitle("ReShare")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(total, current, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(PROGRESS_NOTIFICATION_ID, notification)
    }

    fun hideProgress() {
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
    }

    fun showError(error: ConversionError) {
        // Always show toast - works without permission
        ToastErrorReporter.showError(context, error)

        val (title, message) = when (error) {
            is ConversionError.FileTooLarge ->
                "File too large" to "File exceeds ${error.maxBytes / 1_000_000} MB limit"
            is ConversionError.Timeout ->
                "Conversion timed out" to "The conversion took too long"
            is ConversionError.UnsupportedFormat ->
                "Unsupported format" to "Cannot read this file type"
            is ConversionError.ProcessFailed ->
                "Conversion failed" to (if (error.stderr.isNotEmpty()) error.stderr else "File may be corrupted")
            is ConversionError.InputError ->
                "Input error" to error.message
        }

        // Also show notification for persistence (may be blocked by permission)
        showErrorNotification(title, message)
    }

    private fun showErrorNotification(title: String, message: String) {
        val detailsIntent = Intent(context, ErrorDetailsActivity::class.java).apply {
            putExtra(EXTRA_ERROR_TITLE, title)
            putExtra(EXTRA_ERROR_MESSAGE, message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, detailsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setContentTitle("ReShare - $title")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val PROGRESS_CHANNEL_ID = "conversion_progress"
        private const val ERROR_CHANNEL_ID = "conversion_errors"
        private const val PROGRESS_NOTIFICATION_ID = 1
        private const val ERROR_NOTIFICATION_ID = 2

        const val EXTRA_ERROR_TITLE = "com.reshare.extra.ERROR_TITLE"
        const val EXTRA_ERROR_MESSAGE = "com.reshare.extra.ERROR_MESSAGE"
    }
}
