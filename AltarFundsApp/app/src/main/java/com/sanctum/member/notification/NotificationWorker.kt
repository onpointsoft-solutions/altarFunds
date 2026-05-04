package com.sanctum.member.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sanctum.member.R
import com.sanctum.member.ui.MainActivity

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val data = inputData.keyValueMap
            val notificationType = data["type"] ?: "general"
            val title = data["title"] ?: "Sanctum"
            val message = data["message"] ?: "Background notification"
            val devotionalId = data["devotional_id"] as? String
            val targetUrl = data["target_url"] as? String
            val churchId = data["church_id"] as? String
            val userId = data["user_id"] as? String
            
            Log.d("NotificationWorker", "Processing background notification: $notificationType")
            
            // Check if notification is relevant to user
            if (!isNotificationRelevantToUser(churchId, userId)) {
                Log.d("NotificationWorker", "Skipping notification - not relevant to user")
                return Result.success()
            }
            
            // Show actual notification
            showBackgroundNotification(
                title as String,
                message as String, notificationType as String, devotionalId, targetUrl)
            
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error processing notification", e)
            Result.failure()
        }
    }
    
    private fun isNotificationRelevantToUser(churchId: String?, userId: String?): Boolean {
        val prefs = applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userChurchId = prefs.getString("church_id", null)
        val currentUserId = prefs.getString("user_id", null)
        
        return churchId == null || churchId == userChurchId || userId == currentUserId
    }
    
    private fun showBackgroundNotification(title: String, message: String, type: String, devotionalId: String?, targetUrl: String?) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
            putExtra("title", title)
            putExtra("message", message)
            devotionalId?.let { putExtra("devotional_id", it) }
            targetUrl?.let { putExtra("target_url", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            System.currentTimeMillis().toInt(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(applicationContext, type)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        
        // Add sound and vibration for important notifications
        when (type) {
            "devotional_new", "announcement_posted", "church_event" -> {
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
            }
            "giving_reminder" -> {
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND)
            }
            else -> {
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            }
        }
        
        val notification = notificationBuilder.build()
        
        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel(notificationManager, type)
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            
            Log.d("NotificationWorker", "Background notification shown: $title")
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error showing background notification", e)
        }
    }
    
    private fun createNotificationChannel(notificationManager: NotificationManager, type: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = when (type) {
                "devotional_new", "devotional_shared" -> "devotionals"
                "announcement_posted" -> "announcements"
                "giving_reminder" -> "giving"
                "church_event" -> "events"
                else -> "general"
            }
            
            val channelName = when (type) {
                "devotional_new", "devotional_shared" -> "Devotionals"
                "announcement_posted" -> "Announcements"
                "giving_reminder" -> "Giving"
                "church_event" -> "Church Events"
                else -> "General"
            }
            
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sanctum $channelName notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
}
