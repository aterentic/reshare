package com.reshare.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.reshare.R

class ProgressNotifier(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Conversion Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows document conversion progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgress(message: String = "Converting document...") {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ReShare")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(PROGRESS_NOTIFICATION_ID, notification)
    }

    fun hideProgress() {
        notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
    }

    fun showError(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "conversion_progress"
        private const val PROGRESS_NOTIFICATION_ID = 1
        private const val ERROR_NOTIFICATION_ID = 2
    }
}
