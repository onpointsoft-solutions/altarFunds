package com.sanctum.member.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sanctum.member.MemberApp
import kotlinx.coroutines.delay

class NotificationSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            Log.d("NotificationSyncWorker", "Starting periodic notification sync")
            
            // Check if user is logged in
            val app = applicationContext as MemberApp
            if (!app.tokenManager.isLoggedIn.value) {
                Log.d("NotificationSyncWorker", "User not logged in, skipping sync")
                return Result.success()
            }
            
            // Simulate API call to check for missed notifications
            // In a real implementation, this would call your backend API
            val missedNotifications = checkMissedNotifications()
            
            if (missedNotifications.isNotEmpty()) {
                Log.d("NotificationSyncWorker", "Found ${missedNotifications.size} missed notifications")
                
                // Process each missed notification
                missedNotifications.forEach { notification ->
                    processMissedNotification(notification)
                }
            } else {
                Log.d("NotificationSyncWorker", "No missed notifications found")
            }
            
            // Refresh FCM token if needed
            refreshFCMToken()
            
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationSyncWorker", "Error during notification sync", e)
            Result.failure()
        }
    }
    
    private suspend fun checkMissedNotifications(): List<Map<String, String>> {
        // Simulate API delay
        delay(1000)
        
        // In a real implementation, this would:
        // 1. Call your backend API with last sync timestamp
        // 2. Get list of notifications sent since last sync
        // 3. Return list of notification data
        
        // For demo purposes, return empty list
        return emptyList()
        
        /* Example implementation:
        val apiService = (applicationContext as MemberApp).apiService
        val response = apiService.getMissedNotifications(getLastSyncTimestamp())
        return response.data ?: emptyList()
        */
    }
    
    private fun processMissedNotification(notification: Map<String, String>) {
        // Process the missed notification using the same logic as NotificationService
        val workManager = androidx.work.WorkManager.getInstance(applicationContext)
        val inputData = androidx.work.Data.Builder()
            .putAll(notification)
            .build()
        
        val notificationWork = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(inputData)
            .build()
        
        workManager.enqueue(notificationWork)
        Log.d("NotificationSyncWorker", "Enqueued missed notification: ${notification["title"]}")
    }
    
    private fun refreshFCMToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    val prefs = applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val savedToken = prefs.getString("fcm_token", null)
                    
                    if (token != savedToken) {
                        Log.d("NotificationSyncWorker", "FCM token changed, updating server")
                        prefs.edit().putString("fcm_token", token).apply()
                        // Send new token to server
                        sendTokenToServer(token)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationSyncWorker", "Error refreshing FCM token", e)
        }
    }
    
    private fun sendTokenToServer(token: String) {
        // Implementation to send updated FCM token to server
        Log.d("NotificationSyncWorker", "Sending updated FCM token to server")
    }
    
    private fun getLastSyncTimestamp(): Long {
        val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_sync_timestamp", 0)
    }
    
    private fun updateLastSyncTimestamp() {
        val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply()
    }
}
