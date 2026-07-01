package com.sanctum.member.notification

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sanctum.member.MemberApp
import com.sanctum.member.models.FcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging service.
 *
 * Delivery scenarios:
 *
 *  ┌──────────────────┬────────────────────────────────────────────────────────┐
 *  │ App state        │ What happens                                           │
 *  ├──────────────────┼────────────────────────────────────────────────────────┤
 *  │ FOREGROUND       │ onMessageReceived fires. We call NotificationHelper    │
 *  │                  │ directly — no worker overhead needed.                  │
 *  ├──────────────────┼────────────────────────────────────────────────────────┤
 *  │ BACKGROUND       │ Data-only: onMessageReceived fires → expedited worker. │
 *  │ (process alive)  │ Notification block: system tray handles it via the     │
 *  │                  │ default channel declared in AndroidManifest.           │
 *  ├──────────────────┼────────────────────────────────────────────────────────┤
 *  │ KILLED / STOPPED │ Data-only: FCM wakes the process → onMessageReceived  │
 *  │                  │ → expedited worker.                                    │
 *  │                  │ Notification block: system handles without our code.   │
 *  └──────────────────┴────────────────────────────────────────────────────────┘
 */
class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "NotificationService"
    }

    // ── FCM message received ──────────────────────────────────────────────

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Merge "notification" block + data map.
        // Data keys win so the backend can override display fields.
        val data = mutableMapOf<String, String>()
        remoteMessage.notification?.let { n ->
            n.title?.let { data["title"]   = it }
            n.body?.let  { data["message"] = it }
        }
        data.putAll(remoteMessage.data)

        if (data.isEmpty()) {
            Log.d(TAG, "Empty payload — nothing to display")
            return
        }

        // Relevance check: skip if addressed to a different user / church
        if (!isRelevantToUser(data["church_id"], data["user_id"])) {
            Log.d(TAG, "Filtered out — not relevant to this user")
            return
        }

        val type    = data["type"]    ?: "general"
        val title   = data["title"]   ?: "Sanctum"
        val message = data["message"] ?: "You have a new notification"

        if (isAppInForeground()) {
            // App is visible → show heads-up immediately, skip worker overhead
            Log.d(TAG, "App in foreground — showing notification directly")
            val extras = data.filterKeys { it !in setOf("type", "title", "message", "church_id", "user_id") }
            NotificationHelper.show(
                context  = applicationContext,
                type     = type,
                title    = title,
                message  = message,
                extras   = extras
            )
        } else {
            // App in background / killed → use expedited worker for reliable delivery.
            // Worker name = type+title hash so identical messages don't stack up.
            val workName = "notif_${type}_${(title + message).hashCode()}"
            val inputData = Data.Builder().apply {
                data.forEach { (k, v) -> putString(k, v) }
            }.build()

            val work = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            // KEEP: if the same work is already queued (duplicate FCM delivery),
            // don't enqueue again — prevents duplicate notifications.
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, work)

            Log.d(TAG, "Worker enqueued: $workName  type=$type")
        }
    }

    // ── FCM token refresh ─────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed")

        // Always persist locally first — even if the network call fails,
        // the token is available for the next login / sync cycle.
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit().putString("fcm_token", token).apply()

        val app = applicationContext as? MemberApp ?: return
        if (!app.tokenManager.isLoggedIn.value) {
            Log.d(TAG, "Not logged in — token saved locally, will register on next login")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                val response = app.apiService.registerFcmToken(
                    FcmTokenRequest(token = token, deviceId = deviceId, platform = "android")
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "Rotated FCM token registered with server")
                } else {
                    Log.w(TAG, "Token registration returned ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering rotated FCM token", e)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun isRelevantToUser(churchId: String?, userId: String?): Boolean {
        if (churchId == null && userId == null) return true   // broadcast to everyone
        val prefs        = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val myChurchId   = prefs.getString("church_id", null)
        val myUserId     = prefs.getString("user_id",   null)
        return churchId == myChurchId || userId == myUserId
    }

    /**
     * Returns true if the host app process is currently in the foreground
     * (i.e. at least one activity is resumed).
     *
     * Uses ActivityManager.RunningAppProcessInfo.importance which is the
     * recommended way on all API levels without needing ProcessLifecycleOwner.
     */
    private fun isAppInForeground(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses
            ?.any {
                it.uid == android.os.Process.myUid() &&
                it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } == true
    }
}
