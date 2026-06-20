package com.sanctum.member.notification

import android.content.Context
import android.util.Log
import androidx.work.Data
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
 * Receives FCM messages and either:
 *  - Shows a heads-up notification immediately (foreground / data-only messages), OR
 *  - Delegates to NotificationWorker for background/expedited processing.
 *
 * FCM delivery behaviour:
 *  - App in FOREGROUND  → onMessageReceived() fires; system does NOT auto-display.
 *    We must call NotificationHelper.show() ourselves.
 *  - App in BACKGROUND  → if the message has a "notification" block, the system
 *    auto-displays it using the manifest default channel. If it is DATA-only,
 *    onMessageReceived() fires and we handle it.
 *  - App KILLED         → data-only messages wake up WorkManager via the worker.
 */
class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("NotificationService", "Message received from: ${remoteMessage.from}")

        // ── Extract payload ───────────────────────────────────────────────
        // Merge the FCM "notification" block + data map so we handle both
        val data = mutableMapOf<String, String>()
        remoteMessage.notification?.let { notif ->
            notif.title?.let   { data["title"]   = it }
            notif.body?.let    { data["message"]  = it }
        }
        data.putAll(remoteMessage.data)   // data keys override notification block

        if (data.isEmpty()) return        // nothing to display

        // ── Relevance filter ──────────────────────────────────────────────
        val churchId = data["church_id"]
        val userId   = data["user_id"]
        if (!isRelevantToUser(churchId, userId)) {
            Log.d("NotificationService", "Notification filtered out — not relevant to this user")
            return
        }

        val type    = data["type"]    ?: "general"
        val title   = data["title"]   ?: "Sanctum"
        val message = data["message"] ?: "You have a new notification"

        // ── Show heads-up notification directly ───────────────────────────
        // Using an expedited Worker ensures delivery even if the system is
        // under memory pressure, while still showing immediately via the helper.
        val inputData = Data.Builder().putAll(data as Map<String, Any>).build()

        val work = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(work)
        Log.d("NotificationService", "Notification work enqueued: type=$type title=$title")
    }

    override fun onNewToken(token: String) {
        Log.d("NotificationService", "FCM token refreshed")
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit().putString("fcm_token", token).apply()
        registerTokenWithServer(token)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun isRelevantToUser(churchId: String?, userId: String?): Boolean {
        val prefs         = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userChurchId  = prefs.getString("church_id", null)
        val currentUserId = prefs.getString("user_id",   null)
        return churchId == null || churchId == userChurchId || userId == currentUserId
    }

    private fun registerTokenWithServer(token: String) {
        val app = applicationContext as? MemberApp ?: return
        if (!app.tokenManager.isLoggedIn.value) {
            Log.d("NotificationService", "User not logged in — skipping FCM token registration")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = app.apiService.registerFcmToken(FcmTokenRequest(token = token))
                if (response.isSuccessful) {
                    Log.d("NotificationService", "FCM token registered with server")
                } else {
                    Log.w("NotificationService", "FCM token registration: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NotificationService", "Error registering FCM token", e)
            }
        }
    }
}
