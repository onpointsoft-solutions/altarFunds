package com.sanctum.member.notification

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.sanctum.member.R

/**
 * Processes and displays a single push notification in the background.
 *
 * Marked as expedited so WorkManager runs it promptly even when the app
 * is not in the foreground. getForegroundInfo() is the mandatory fallback
 * for devices that cannot grant expedited quota.
 *
 * Retry policy: returns Result.retry() on transient errors so WorkManager
 * will back off and re-attempt rather than silently dropping the notification.
 */
class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"

        // Input data keys — must match what NotificationService puts in
        const val KEY_TYPE          = "type"
        const val KEY_TITLE         = "title"
        const val KEY_MESSAGE       = "message"
        const val KEY_CHURCH_ID     = "church_id"
        const val KEY_USER_ID       = "user_id"
        const val KEY_DEVOTIONAL_ID = "devotional_id"
        const val KEY_TARGET_URL    = "target_url"
    }

    /**
     * Foreground info shown while the worker is executing on devices where
     * expedited quota is unavailable. Kept silent / low-priority so it does
     * not interfere with the real notification we are about to post.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        NotificationHelper.createChannels(applicationContext)

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationHelper.CH_GENERAL
        )
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Sanctum")
            .setContentText("Delivering your notification…")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        return ForegroundInfo(NotificationHelper.NID_PROCESSING, notification)
    }

    override suspend fun doWork(): Result {
        // Honour WorkManager's retry limit — give up after too many attempts
        if (runAttemptCount >= 3) {
            Log.w(TAG, "Giving up after $runAttemptCount attempts")
            return Result.failure()
        }

        return try {
            val type         = inputData.getString(KEY_TYPE)          ?: "general"
            val title        = inputData.getString(KEY_TITLE)         ?: "Sanctum"
            val message      = inputData.getString(KEY_MESSAGE)       ?: "You have a new notification"
            val churchId     = inputData.getString(KEY_CHURCH_ID)
            val userId       = inputData.getString(KEY_USER_ID)
            val devotionalId = inputData.getString(KEY_DEVOTIONAL_ID)
            val targetUrl    = inputData.getString(KEY_TARGET_URL)

            Log.d(TAG, "Processing notification: type=$type title=$title attempt=$runAttemptCount")

            // Skip notifications that don't belong to this user / church
            if (!isRelevantToUser(churchId, userId)) {
                Log.d(TAG, "Notification skipped — not relevant to user")
                return Result.success()
            }

            val extras = buildMap<String, String> {
                devotionalId?.let { put(KEY_DEVOTIONAL_ID, it) }
                targetUrl?.let    { put(KEY_TARGET_URL,    it) }
            }

            NotificationHelper.show(
                context  = applicationContext,
                type     = type,
                title    = title,
                message  = message,
                extras   = extras
            )

            Log.d(TAG, "Notification delivered successfully: $title")
            Result.success()

        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — retrying won't help
            Log.w(TAG, "POST_NOTIFICATIONS permission denied, dropping notification", e)
            Result.failure()
        } catch (e: Exception) {
            // Transient error — let WorkManager retry with back-off
            Log.e(TAG, "Transient error processing notification, will retry", e)
            Result.retry()
        }
    }

    /**
     * Returns true when this notification should be shown to the current user.
     * A notification with no churchId / userId targets everyone and is always shown.
     */
    private fun isRelevantToUser(churchId: String?, userId: String?): Boolean {
        // No targeting constraints → show to everyone
        if (churchId == null && userId == null) return true

        val prefs         = applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userChurchId  = prefs.getString("church_id", null)
        val currentUserId = prefs.getString("user_id",   null)

        return churchId == userChurchId || userId == currentUserId
    }
}