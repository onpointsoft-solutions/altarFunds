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
 * Central notification utility.
 *
 * Responsibilities:
 *  - Create and manage all notification channels (idempotent, call freely).
 *  - Build and post heads-up notifications with correct priority/channel.
 *  - Provide stable, collision-free notification IDs.
 *
 * Heads-up requirements (all must be true simultaneously):
 *  1. Channel importance ≥ IMPORTANCE_HIGH          (Android 8+)
 *  2. Builder priority  = PRIORITY_HIGH
 *  3. setDefaults(DEFAULT_ALL)                       (triggers sound + vibration)
 *  4. Channel ID in builder matches a registered channel
 *  5. POST_NOTIFICATIONS permission granted          (Android 13+)
 *  6. App not in DND / battery-saver / full-screen
 */
object NotificationHelper {

    // ── Stable channel IDs ────────────────────────────────────────────────
    const val CH_DEVOTIONALS   = "ch_devotionals"
    const val CH_ANNOUNCEMENTS = "ch_announcements"
    const val CH_EVENTS        = "ch_events"
    const val CH_GIVING        = "ch_giving"
    const val CH_GENERAL       = "ch_general"

    // Reserved ID for the silent foreground-service notification while a worker runs
    const val NID_PROCESSING = 9999

    // Tag prefix used with notify(tag, id) to make IDs unique per type
    private const val NOTIF_TAG = "sanctum"
    private const val TAG       = "NotificationHelper"

    // ── Channel map ───────────────────────────────────────────────────────

    fun channelIdForType(type: String): String = when (type) {
        "devotional_new",
        "devotional_shared"                    -> CH_DEVOTIONALS
        "announcement_posted",
        "announcement"                         -> CH_ANNOUNCEMENTS
        "church_event"                         -> CH_EVENTS
        "giving_reminder",
        "payment_received"                     -> CH_GIVING
        else                                   -> CH_GENERAL
    }

    // ── Channel creation ──────────────────────────────────────────────────

    /**
     * Create / verify all notification channels.
     * Safe to call on every app start — the OS ignores already-registered channels.
     * Must be called before posting any notification on Android O+.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val definitions = listOf(
            Triple(CH_DEVOTIONALS,   "Devotionals",    "New devotionals and shares"),
            Triple(CH_ANNOUNCEMENTS, "Announcements",  "Church announcements and notices"),
            Triple(CH_EVENTS,        "Church Events",  "Upcoming events and activities"),
            Triple(CH_GIVING,        "Giving",         "Giving reminders and confirmations"),
            Triple(CH_GENERAL,       "General",        "Other Sanctum updates"),
        )

        val channels = definitions.map { (id, name, desc) ->
            NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = desc
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
        }

        nm.createNotificationChannels(channels)
        Log.d(TAG, "All notification channels created/verified")
    }

    // ── Notification posting ──────────────────────────────────────────────

    /**
     * Build and post a heads-up notification.
     *
     * Uses a **tag + stable ID** strategy:
     *  - tag  = "sanctum_$type"       → groups by type so updates replace each other
     *  - id   = stable hash of title  → same logical notification replaces itself
     *
     * This prevents the overflow / collision problem with `currentTimeMillis().toInt()`.
     *
     * @return true if the notification was posted, false if suppressed.
     */
    fun show(
        context  : Context,
        type     : String,
        title    : String,
        message  : String,
        extras   : Map<String, String?> = emptyMap(),
        // Callers may pass a stable ID; defaults to hash of title for dedup
        notifId  : Int = stableId(title),
    ): Boolean {
        val nmc = NotificationManagerCompat.from(context)
        if (!nmc.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled — skipping post")
            return false
        }

        createChannels(context)

        val channelId = channelIdForType(type)
        val tag       = "${NOTIF_TAG}_$type"

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("title",             title)
            putExtra("message",           message)
            extras.forEach { (k, v) -> v?.let { putExtra(k, it) } }
        }

        // Unique request code per notification so each gets its own back-stack
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Show badge on launcher icon
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .build()

        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // notify(tag, id) — tag isolates types, id replaces same notification
            nm.notify(tag, notifId, notification)
            Log.d(TAG, "Posted: tag=$tag id=$notifId channel=$channelId title=$title")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS denied", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification", e)
            false
        }
    }

    /**
     * Convert a string to a stable positive int ID.
     * Uses Math.abs to avoid negative IDs that confuse some devices.
     * Guaranteed non-zero (falls back to 1 on the rare hash==0 case).
     */
    fun stableId(key: String): Int {
        val h = Math.abs(key.hashCode())
        return if (h == 0) 1 else h
    }
}
