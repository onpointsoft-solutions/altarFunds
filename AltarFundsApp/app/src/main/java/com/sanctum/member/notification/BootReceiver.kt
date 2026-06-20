package com.sanctum.member.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.sanctum.member.MemberApp
import com.sanctum.member.models.FcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Restarts notification infrastructure after:
 *  - Device reboot          (ACTION_BOOT_COMPLETED)
 *  - App self-update        (ACTION_MY_PACKAGE_REPLACED)
 *
 * Both events cancel all WorkManager chains, so we must re-enqueue here.
 *
 * FCM token refresh now uses `await()` inside a supervised coroutine so
 * the async work cannot be orphaned if the process is killed early.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG              = "BootReceiver"
        private const val WORK_SYNC_NAME   = "notification_sync"
        private const val SYNC_INTERVAL_H  = 6L
        private const val PREFS_USER       = "user_prefs"
        private const val KEY_FCM_TOKEN    = "fcm_token"
    }

    /**
     * goAsync() is NOT used here because our coroutine work is short
     * (token fetch + one network call). BroadcastReceiver has ~10 s before
     * ANR on modern Android; FCM token retrieval is typically < 1 s.
     * If that ever changes, wrap in goAsync() + pendingResult.finish().
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Restarting notification services after boot/update")
                refreshFcmTokenAndRegister(context)
                schedulePeriodicSync(context)
            }
        }
    }

    /**
     * Retrieves a fresh FCM token and re-registers it with the backend.
     *
     * Uses a SupervisorJob scope so a failure in the network call does not
     * cancel the token-persistence step — token is always saved locally even
     * if the server call fails.
     */
    private fun refreshFcmTokenAndRegister(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM token retrieved after boot/update")

                // Always persist locally so the sync worker can detect rotation
                context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_FCM_TOKEN, token)
                    .apply()

                // Only push to server if the user is logged in
                val app = context.applicationContext as? MemberApp ?: return@launch
                if (!app.tokenManager.isLoggedIn.value) {
                    Log.d(TAG, "User not logged in — skipping server FCM registration")
                    return@launch
                }

                val response = app.apiService.registerFcmToken(FcmTokenRequest(token = token))
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token re-registered after boot/update")
                } else {
                    Log.w(TAG, "FCM re-registration returned HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing/re-registering FCM token", e)
            }
        }
    }

    /**
     * Re-enqueues the periodic notification sync worker.
     *
     * Policy: UPDATE — replaces any stale chain left over from before the
     * reboot/update. Using KEEP here would silently skip re-scheduling if
     * WorkManager somehow kept a phantom record of the old chain.
     *
     * Constraints: network required + battery not low, to be a good
     * background citizen while still ensuring the sync eventually runs.
     */
    private fun schedulePeriodicSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                SYNC_INTERVAL_H, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_SYNC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,   // replace stale chain after boot
                request
            )

            Log.d(TAG, "Periodic notification sync scheduled (every ${SYNC_INTERVAL_H}h)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic sync", e)
        }
    }
}