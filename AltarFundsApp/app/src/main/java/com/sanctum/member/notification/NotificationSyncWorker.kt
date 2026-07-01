package com.sanctum.member.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import com.sanctum.member.MemberApp
import com.sanctum.member.models.FcmTokenRequest
import com.sanctum.member.models.PushNotification
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Periodic background worker (every 6 hours) that:
 *
 *  1. Fetches unread notifications from the server that arrived since the last
 *     successful sync and displays them — catches anything missed while the app
 *     was offline or killed.
 *  2. Detects FCM token rotation and re-registers the fresh token with the server.
 *  3. Persists a "last sync" epoch-ms timestamp so successive runs only look at
 *     new items.
 *
 * Key improvements over the original:
 *  - Timestamp comparison now uses the model's `createdAt` field directly (no
 *    reflection). An ISO-8601 → epoch-ms converter handles multiple formats.
 *  - FCM token refresh uses `await()` inside the worker's own coroutine scope so
 *    it cannot be orphaned or killed between steps.
 *  - Worker retries once on transient failure, then gives up cleanly.
 */
class NotificationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG           = "NotificationSyncWorker"
        private const val PREFS_SYNC    = "sync_prefs"
        private const val PREFS_USER    = "user_prefs"
        private const val KEY_LAST_SYNC = "last_notification_sync_ms"
        private const val KEY_FCM_TOKEN = "fcm_token"

        /** ISO-8601 formats the backend may return. */
        private val ISO_FORMATS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        )

        fun parseIso8601ToMillis(dateStr: String?): Long {
            if (dateStr.isNullOrBlank()) return 0L
            for (fmt in ISO_FORMATS) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    return sdf.parse(dateStr)?.time ?: continue
                } catch (_: Exception) { /* try next format */ }
            }
            return 0L
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting sync (attempt $runAttemptCount)")

            val app = applicationContext as MemberApp
            if (!app.tokenManager.isLoggedIn.value) {
                Log.d(TAG, "Not logged in — skipping sync")
                return Result.success()
            }

            // Step 1 — Missed notifications
            fetchAndDisplayMissed(app)

            // Step 2 — FCM token rotation check
            refreshFcmTokenIfNeeded(app)

            // Step 3 — Persist sync timestamp
            saveLastSyncTimestamp()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            if (runAttemptCount < 1) Result.retry() else Result.failure()
        }
    }

    // ── Step 1: missed notifications ──────────────────────────────────────

    private suspend fun fetchAndDisplayMissed(app: MemberApp) {
        val lastSyncMs = loadLastSyncTimestamp()

        try {
            val response = app.apiService.getNotifications()
            if (!response.isSuccessful || response.body() == null) {
                Log.w(TAG, "getNotifications() → HTTP ${response.code()}")
                return
            }

            val notifications: List<PushNotification> = response.body()!!.results
                .filter { !it.isRead }
                .filter { parseIso8601ToMillis(it.createdAt) > lastSyncMs }

            Log.d(TAG, "Missed notifications to display: ${notifications.size}")

            notifications.forEach { n ->
                val extras = buildMap<String, String> {
                    n.targetUrl?.let { put("target_url", it) }
                    n.data?.forEach { (k, v) -> put(k, v.toString()) }
                    put(NotificationWorker.KEY_NOTIF_ID, n.id.toString())
                }
                NotificationHelper.show(
                    context  = applicationContext,
                    type     = n.notificationType,
                    title    = n.title,
                    message  = n.message,
                    extras   = extras,
                    notifId  = NotificationHelper.stableId(n.id.toString()),
                )
                Log.d(TAG, "Displayed missed: '${n.title}'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching missed notifications", e)
        }
    }

    // ── Step 2: FCM token rotation ────────────────────────────────────────

    private suspend fun refreshFcmTokenIfNeeded(app: MemberApp) {
        try {
            val freshToken = FirebaseMessaging.getInstance().token.await()
            val prefs      = applicationContext.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            val savedToken = prefs.getString(KEY_FCM_TOKEN, null)

            if (freshToken == savedToken) {
                Log.d(TAG, "FCM token unchanged")
                return
            }

            Log.d(TAG, "FCM token rotated — updating server")
            prefs.edit().putString(KEY_FCM_TOKEN, freshToken).apply()

            val deviceId = android.provider.Settings.Secure.getString(
                applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            val response = app.apiService.registerFcmToken(
                FcmTokenRequest(token = freshToken, deviceId = deviceId, platform = "android")
            )
            if (response.isSuccessful) {
                Log.d(TAG, "Rotated token registered")
            } else {
                Log.w(TAG, "Token registration → HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "FCM token refresh error", e)
        }
    }

    // ── Timestamp persistence ─────────────────────────────────────────────

    private fun loadLastSyncTimestamp(): Long =
        applicationContext
            .getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC, 0L)

    private fun saveLastSyncTimestamp() {
        applicationContext
            .getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Sync timestamp saved")
    }
}
