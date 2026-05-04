package com.sanctum.member.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.sanctum.member.R
import com.sanctum.member.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            val workManager = WorkManager.getInstance(applicationContext)
            val inputData = androidx.work.Data.Builder()
                .putAll(remoteMessage.data)
                .build()
            val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(inputData)
                .build()
            workManager.enqueue(notificationWork)
            handleNotification(remoteMessage.data, this)
        }
    }
    
    override fun onNewToken(token: String) {
        // Save FCM token to server
        sendTokenToServer(token)
    }
    
    private fun handleNotification(data: Map<String, String>,context: Context) {
        val notificationType = data["type"] ?: "general"
        val title = data["title"] ?: "Sanctum"
        val message = data["message"] ?: "You have a new notification"
        val devotionalId = data["devotional_id"]
        val targetUrl = data["target_url"]
        val churchId = data["church_id"]
        val userId = data["user_id"]
        
        // Check if notification is relevant to user's church
        if (!isNotificationRelevantToUser(churchId, userId, context)) {
            Log.d("NotificationService", "Skipping notification - not relevant to user's church")
            return
        }
        
        when (notificationType) {
            "devotional_shared" -> {
                showDevotionalSharedNotification(title, message, devotionalId,context)
            }
            "devotional_new" -> {
                showNewDevotionalNotification(title, message, devotionalId,context)
            }
            "announcement_posted" -> {
                showAnnouncementNotification(title, message,context)
            }
            "giving_reminder" -> {
                showGivingReminderNotification(title, message,context)
            }
            "church_event" -> {
                showChurchEventNotification(title, message,context)
            }
            "prayer_request" -> {
                showPrayerRequestNotification(title, message,context)
            }
            else -> {
                showGeneralNotification(title, message, targetUrl,context)
            }
        }
    }
    
    private fun isNotificationRelevantToUser(churchId: String?, userId: String?, context: Context): Boolean {
        // Get current user's church ID and user ID from preferences
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userChurchId = prefs.getString("church_id", null)
        val currentUserId = prefs.getString("user_id", null)
        
        // Notification is relevant if:
        // 1. No specific church/user filters (general notification)
        // 2. Matches user's church
        // 3. Targets specific user
        return churchId == null || churchId == userChurchId || userId == currentUserId
    }
    
    private fun showDevotionalSharedNotification(title: String, message: String, devotionalId: String?,context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "devotional_shared")
            putExtra("devotional_id", devotionalId)
            putExtra("title", title)
            putExtra("message", message)
        }
        
        showNotification(
            intent, "devotional_shared",
            context =context
        )
    }
    
    private fun showNewDevotionalNotification(title: String, message: String, devotionalId: String?,context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "devotional_new")
            putExtra("devotional_id", devotionalId)
            putExtra("title", title)
            putExtra("message", message)
        }
        
        showNotification(intent, "devotional_new",context)
    }
    
    private fun showAnnouncementNotification(title: String, message: String,context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "announcement")
            putExtra("title", title)
            putExtra("message", message)
        }
        
        showNotification(intent, "announcement",context)
    }
    
    private fun showGeneralNotification(title: String, message: String, targetUrl: String?,context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "general")
            putExtra("title", title)
            putExtra("message", message)
            targetUrl?.let { putExtra("target_url", it) }
        }
        
        showNotification(intent, "general",context)
    }
    
    private fun showNotification(intent: Intent, channelId: String,context: Context) {
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(intent.getStringExtra("title") ?: "Sanctum")
            .setContentText(intent.getStringExtra("message") ?: "New notification")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setOngoing(true)
        }
        
        val notification = notificationBuilder.build()
        
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel(notificationManager)
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sanctum_notifications",
                "Sanctum Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from Sanctum app"
                enableLights(true)
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showGivingReminderNotification(title: String, message: String,context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "giving_reminder")
            putExtra("title", title)
            putExtra("message", message)
        }
        
        showNotification(intent, "giving_reminder",context)
    }
    
    private fun showChurchEventNotification(title: String, message: String,context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "church_event")
            putExtra("title", title)
            putExtra("message", message)
        }
        
        showNotification(intent, "church_event",context)
    }
    
    private fun showPrayerRequestNotification(title: String, message: String,context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "prayer_request")
            putExtra("title", title)
            putExtra("message", message)
        }
        
        showNotification(intent, "prayer_request",context)
    }
    
    private fun sendTokenToServer(token: String) {
        // This would make an API call to save the token
        // For now, just log it
        Log.d("NotificationService", "FCM Token: $token")
    }
}
