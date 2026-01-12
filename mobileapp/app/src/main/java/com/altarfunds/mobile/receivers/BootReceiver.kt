package com.altarfunds.mobile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot receiver triggered: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                // Start sync service after boot or app update
                startSyncService(context)
                scheduleNotifications(context)
            }
        }
    }

    private fun startSyncService(context: Context) {
        try {
            val syncIntent = Intent(context, com.altarfunds.mobile.services.SyncService::class.java).apply {
                action = "SYNC_ALL"
            }
            context.startService(syncIntent)
            Log.d(TAG, "Sync service started after boot")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sync service", e)
        }
    }

    private fun scheduleNotifications(context: Context) {
        // TODO: Schedule periodic notifications
        Log.d(TAG, "Scheduling notifications after boot")
    }
}
