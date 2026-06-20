package com.sanctum.member.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sanctum.member.R
import com.sanctum.member.ui.MainActivity

/**
 * Single shared helper used by NotificationService, NotificationWorker,
 * and NotificationSyncWorker to create channels and post notifications.
 *
 * Heads-up (peek) display requirements — ALL of the following must be true:
 *  1. Channel importance = IMPORTANCE_HIGH  (Android O+)
 *  2. Builder priority  = PRIORITY_HIGH
 *  3. setDefaults() includes DEFAULT_SOUND or DEFAULT_ALL
 *  4. Channel ID in builder exactly matches a registered channel ID
 *  5. POST_NOTIFICATIONS permission granted (Android 13+)
 *  6. App must not be in full-screen / DND / battery-saver blocking mode
 */
object NotificationHelper {

    // ── Stable channel IDs ────────────────────────────────────────────────
    const val CH_DEVOTIONALS   = "ch_devotionals"
    const val CH_ANNOUNCEMENTS = "ch_announcements"
    const val CH_EVENTS        = "ch_events"
    const val CH_GIVING        = "ch_giving"
    const val CH_GENERAL       = "ch_general"

    // Used for the silent foreground notification while a worker is executing
    const val NID_PROCESSING = 9999

    private const val TAG = "NotificationHelper"

    /**
     * Maps an FCM notification type string to the correct stable channel ID.
     */
    fun channelIdForType(type: String): String = when (type) {
        "devotional_new", "devotional_shared"  -> CH_DEVOTIONALS
        "announcement_posted", "announcement"  -> CH_ANNOUNCEMENTS
        "church_event"                          -> CH_EVENTS
        "giving_reminder"                       -> CH_GIVING
        else                                    -> CH_GENERAL
    }

    /**
     * Create all notification channels.
     * Safe to call multiple times — the OS ignores already-registered channels.
     * Must be called before posting any notification on Android O+.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CH_DEVOTIONALS, "Devotionals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New devotionals and shares"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CH_ANNOUNCEMENTS, "Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Church announcements and notices"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CH_EVENTS, "Church Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upcoming events and activities"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CH_GIVING, "Giving",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Giving reminders and confirmations"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CH_GENERAL, "General",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Other Sanctum updates"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
        )

        nm.createNotificationChannels(channels)
        Log.d(TAG, "All notification channels created/verified")
    }

    /**
     * Build and post a heads-up notification.
     *
     * Returns `true` if the notification was posted, `false` if it was
     * suppressed (permission denied or notifications disabled by the user).
     *
     * @param context   Application context
     * @param type      FCM notification type string (used to pick the channel)
     * @param title     Notification title
     * @param message   Notification body text
     * @param extras    Extra key→value pairs forwarded to the tap intent
     * @param notifId   Notification ID (defaults to timestamp-based unique ID)
     */
    fun show(
        context: Context,
        type: String,
        title: String,
        message: String,
        extras: Map<String, String?> = emptyMap(),
        notifId: Int = System.currentTimeMillis().toInt()
    ): Boolean {
        // ── Guard: are notifications enabled at all? ──────────────────────
        // NotificationManagerCompat.areNotificationsEnabled() covers both the
        // Android 13 POST_NOTIFICATIONS runtime permission and the per-app
        // toggle in system settings — one check handles both cases.
        val nmc = NotificationManagerCompat.from(context)
        if (!nmc.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled for this app — skipping post")
            return false
        }

        // Ensure channels exist before building (idempotent)
        createChannels(context)

        val channelId = channelIdForType(type)

        // Build tap intent — opens MainActivity with notification data attached
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("title",             title)
            putExtra("message",           message)
            extras.forEach { (k, v) -> v?.let { putExtra(k, it) } }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // These three together guarantee heads-up on both pre-O and O+
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notifId, notification)
            Log.d(TAG, "Notification posted: type=$type id=$notifId channel=$channelId")
            true
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS denied on Android 13+ — areNotificationsEnabled()
            // should have caught this, but handle defensively just in case.
            Log.w(TAG, "SecurityException posting notification (POST_NOTIFICATIONS denied)")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification", e)
            false
        }
    }
}