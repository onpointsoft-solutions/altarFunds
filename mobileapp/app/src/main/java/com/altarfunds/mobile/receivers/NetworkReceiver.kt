package com.altarfunds.mobile.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

class NetworkReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Network state changed")
        
        if (isNetworkAvailable(context)) {
            // Network is available, sync data
            onNetworkAvailable(context)
        } else {
            // Network is not available
            onNetworkUnavailable(context)
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun onNetworkAvailable(context: Context) {
        Log.d(TAG, "Network available - starting sync")
        
        try {
            val syncIntent = Intent(context, com.altarfunds.mobile.services.SyncService::class.java).apply {
                action = "SYNC_ALL"
            }
            context.startService(syncIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sync on network available", e)
        }
    }

    private fun onNetworkUnavailable(context: Context) {
        Log.d(TAG, "Network unavailable")
        // TODO: Handle network unavailable scenario
    }
}
