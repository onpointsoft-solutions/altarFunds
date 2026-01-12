package com.altarfunds.mobile.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseMessagingService"
        private const val CHANNEL_ID = "altar_funds_channel"
        private const val CHANNEL_NAME = "AltarFunds Notifications"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            handleNotificationMessage(it)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "AltarFunds"
        val message = data["message"] ?: "You have a new notification"
        
        // Show notification
        showNotification(title, message)
    }

    private fun handleNotificationMessage(notification: RemoteMessage.Notification) {
        val title = notification.title ?: "AltarFunds"
        val body = notification.body ?: "You have a new notification"
        
        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Receive updates about your giving and church activities"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // For now, just log the notification
        // In a real implementation, you would create and show a notification here
        Log.d(TAG, "Notification: $title - $message")
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Send token to your server
        Log.d(TAG, "Sending token to server: $token")
    }
}
