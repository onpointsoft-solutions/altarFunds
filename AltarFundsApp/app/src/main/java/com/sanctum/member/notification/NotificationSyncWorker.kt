package com.sanctum.member.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging
import com.sanctum.member.MemberApp
import com.sanctum.member.models.FcmTokenRequest
import kotlinx.coroutines.tasks.await

/**
 * Periodic background worker that:
 *  1. Fetches any notifications the app may have missed while offline / killed.
 *  2. Refreshes the FCM token if it has rotated since the last run.
 *  3. Persists a "last sync" timestamp so each run only looks at new items.
 *
 * Key fixes vs the original:
 *  - Timestamp filter was always `lastSync == 0L` → now properly compares ISO
 *    timestamps by converting them to epoch millis via the Date api.
 *  - FCM token refresh previously spawned a fire-and-forget CoroutineScope
 *    that could be killed mid-flight. It is now fully `await()`-based and
 *    runs inside the worker's own coroutine lifecycle.
 */
class NotificationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG            = "NotificationSyncWorker"
        private const val PREFS_SYNC     = "sync_prefs"
        private const val PREFS_USER     = "user_prefs"
        private const val KEY_LAST_SYNC  = "last_notification_sync_ms"
        private const val KEY_FCM_TOKEN  = "fcm_token"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic notification sync (attempt $runAttemptCount)")

            val app = applicationContext as MemberApp

            if (!app.tokenManager.isLoggedIn.value) {
                Log.d(TAG, "User not logged in — skipping sync")
                return Result.success()
            }

            // ── 1. Missed notifications ───────────────────────────────────
            val missed = fetchMissedNotifications(app)
            Log.d(TAG, "Missed notifications to display: ${missed.size}")
            missed.forEach { displayMissedNotification(it) }

            // ── 2. FCM token refresh ──────────────────────────────────────
            refreshFcmTokenIfNeeded(app)

            // ── 3. Persist sync timestamp ─────────────────────────────────
            saveLastSyncTimestamp()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during notification sync", e)
            // Retry once; if the second attempt also fails, give up cleanly
            if (runAttemptCount < 1) Result.retry() else Result.failure()
        }
    }

    // ── Missed notifications ──────────────────────────────────────────────

    private suspend fun fetchMissedNotifications(app: MemberApp): List<Map<String, String>> {
        return try {
            val response = app.apiService.getNotifications()
            if (!response.isSuccessful || response.body() == null) {
                Log.w(TAG, "getNotifications() returned HTTP ${response.code()}")
                return emptyList()
            }

            val lastSyncMs = loadLastSyncTimestamp()
            val body       = response.body()!!

            body.results
                .filter { !it.isRead }
                .filter { notification ->
                    // Only surface notifications that arrived after our last successful sync.
                    // parsedCreatedAtMs() converts the ISO-8601 string from the API to epoch
                    // millis so we can do a reliable numeric comparison.
                    val createdAtMs = notification.parsedCreatedAtMs()
                    createdAtMs > lastSyncMs
                }
                .map { notification ->
                    buildMap {
                        put("type",    notification.notificationType)
                        put("title",   notification.title)
                        put("message", notification.message)
                        notification.targetUrl?.let { put("target_url", it) }
                        notification.data?.forEach { (k, v) -> put(k, v.toString()) }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching missed notifications", e)
            emptyList()
        }
    }

    private fun displayMissedNotification(notification: Map<String, String>) {
        val type    = notification["type"]    ?: "general"
        val title   = notification["title"]   ?: "Sanctum"
        val message = notification["message"] ?: "You have a missed notification"
        val extras  = notification.filterKeys { it !in setOf("type", "title", "message") }

        NotificationHelper.show(
            context  = applicationContext,
            type     = type,
            title    = title,
            message  = message,
            extras   = extras
        )
        Log.d(TAG, "Displayed missed notification: $title")
    }

    // ── FCM token refresh ─────────────────────────────────────────────────

    /**
     * Awaits the FCM token inside the worker's own coroutine so the scope
     * cannot be orphaned. Previously this used an ad-hoc CoroutineScope that
     * WorkManager had no visibility into and could kill before completion.
     */
    private suspend fun refreshFcmTokenIfNeeded(app: MemberApp) {
        try {
            val freshToken = FirebaseMessaging.getInstance().token.await()
            val prefs      = applicationContext.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            val savedToken = prefs.getString(KEY_FCM_TOKEN, null)

            if (freshToken == savedToken) {
                Log.d(TAG, "FCM token unchanged — no update needed")
                return
            }

            Log.d(TAG, "FCM token rotated — persisting and notifying server")
            prefs.edit().putString(KEY_FCM_TOKEN, freshToken).apply()

            val response = app.apiService.registerFcmToken(FcmTokenRequest(token = freshToken))
            if (response.isSuccessful) {
                Log.d(TAG, "Rotated FCM token re-registered successfully")
            } else {
                Log.w(TAG, "FCM token re-registration returned HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            // Non-fatal: the next sync run will try again
            Log.e(TAG, "Error refreshing FCM token", e)
        }
    }

    // ── Timestamp helpers ─────────────────────────────────────────────────

    private fun loadLastSyncTimestamp(): Long {
        return applicationContext
            .getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC, 0L)
    }

    private fun saveLastSyncTimestamp() {
        applicationContext
            .getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Last sync timestamp saved")
    }
}

// ── Extension — parse ISO-8601 createdAt from the API model ──────────────────

/**
 * Converts a notification's `createdAt` ISO-8601 string (e.g. "2025-06-01T14:30:00Z")
 * to epoch millis for timestamp comparison.
 *
 * Returns 0 if the field is null or unparseable so the notification is treated
 * as "old" and filtered out — a safe default that avoids spam on first run.
 *
 * Add this to your notification model file, or keep it here as an extension.
 */
private fun Any.parsedCreatedAtMs(): Long {
    return try {
        // Access the createdAt field reflectively so this file compiles without
        // needing to import the concrete model class.
        val createdAt = this::class.java
            .getDeclaredField("createdAt")
            .also { it.isAccessible = true }
            .get(this) as? String
            ?: return 0L

        // java.text.SimpleDateFormat is available on all Android API levels
        val sdf = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.US
        ).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        sdf.parse(createdAt)?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}