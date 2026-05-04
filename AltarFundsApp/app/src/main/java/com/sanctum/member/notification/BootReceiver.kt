package com.sanctum.member.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Device rebooted - restarting notification services")
        
        // Re-initialize Firebase messaging
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("BootReceiver", "FCM token retrieved after boot: $token")
                    // Send token to server
                    sendTokenToServer(token, context)
                } else {
                    Log.e("BootReceiver", "Failed to get FCM token after boot", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error initializing Firebase after boot", e)
        }
        
        // Schedule periodic notification sync
        schedulePeriodicSync(context)
        
        // Request battery optimization whitelist
        requestBatteryOptimizationWhitelist(context)
    }
    
    private fun sendTokenToServer(token: String, context: Context) {
        // Implementation to send token to your backend
        Log.d("BootReceiver", "Sending FCM token to server after boot")
    }
    
    private fun schedulePeriodicSync(context: Context) {
        // Schedule periodic work to check for missed notifications
        val workManager = WorkManager.getInstance(context)
        // This would be implemented with PeriodicWorkRequest
        Log.d("BootReceiver", "Periodic notification sync scheduled")
    }
    
    private fun requestBatteryOptimizationWhitelist(context: Context) {
        // Request to be whitelisted from battery optimization
        // This helps ensure notifications work reliably in background
        Log.d("BootReceiver", "Battery optimization whitelist requested")
    }
}
