package com.unfold.core.ui.components.hud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteText = intent.getStringExtra("noteText") ?: "Reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "unfold_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Unfold Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            // .setSmallIcon(R.drawable.ic_notification) // You'd use a real icon here
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Unfold Reminder")
            .setContentText(noteText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(noteText.hashCode(), notification)
    }
}
