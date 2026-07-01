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
 * Runs as an expedited worker so WorkManager schedules it immediately even
 * when the system is under memory pressure. [getForegroundInfo] provides the
 * mandatory silent foreground notification fallback for devices where expedited
 * quota is exhausted.
 *
 * Deduplication: the work name in [NotificationService] uses
 * [ExistingWorkPolicy.KEEP], so a second identical FCM delivery is silently
 * dropped before this worker even starts.
 *
 * Retry policy: transient exceptions return [Result.retry] (max 3 attempts
 * via [runAttemptCount] guard). Permanent failures (e.g. permission denied)
 * return [Result.failure] immediately.
 */
class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"

        // Keys that match what NotificationService writes to inputData
        const val KEY_TYPE          = "type"
        const val KEY_TITLE         = "title"
        const val KEY_MESSAGE       = "message"
        const val KEY_CHURCH_ID     = "church_id"
        const val KEY_USER_ID       = "user_id"
        const val KEY_DEVOTIONAL_ID = "devotional_id"
        const val KEY_TARGET_URL    = "target_url"
        const val KEY_NOTIF_ID      = "notification_id"   // optional stable DB id from backend
    }

    // ── Foreground info (required for expedited workers) ──────────────────

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
            .setAutoCancel(false)
            .build()

        return ForegroundInfo(NotificationHelper.NID_PROCESSING, notification)
    }

    // ── Main work ─────────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        if (runAttemptCount >= 3) {
            Log.w(TAG, "Max retry count reached — dropping notification")
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
            val backendId    = inputData.getString(KEY_NOTIF_ID)

            Log.d(TAG, "Processing: type=$type title='$title' attempt=$runAttemptCount")

            if (!isRelevantToUser(churchId, userId)) {
                Log.d(TAG, "Skipped — not relevant to this user")
                return Result.success()
            }

            // Use the backend notification ID for stable dedup if available,
            // otherwise hash the title.
            val notifId = backendId?.toIntOrNull()?.let { Math.abs(it) }
                ?: NotificationHelper.stableId(title)

            val extras = buildMap<String, String> {
                devotionalId?.let { put(KEY_DEVOTIONAL_ID, it) }
                targetUrl?.let    { put(KEY_TARGET_URL,    it) }
                backendId?.let    { put(KEY_NOTIF_ID,      it) }
            }

            NotificationHelper.show(
                context  = applicationContext,
                type     = type,
                title    = title,
                message  = message,
                extras   = extras,
                notifId  = notifId,
            )

            Log.d(TAG, "Delivered: '$title' (id=$notifId)")
            Result.success()

        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted — retrying won't fix this
            Log.w(TAG, "POST_NOTIFICATIONS denied — dropping notification", e)
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Transient error — will retry (attempt=$runAttemptCount)", e)
            Result.retry()
        }
    }

    // ── Relevance ─────────────────────────────────────────────────────────

    private fun isRelevantToUser(churchId: String?, userId: String?): Boolean {
        if (churchId == null && userId == null) return true
        val prefs   = applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val myChurch = prefs.getString("church_id", null)
        val myUser   = prefs.getString("user_id",   null)
        return churchId == myChurch || userId == myUser
    }
}
