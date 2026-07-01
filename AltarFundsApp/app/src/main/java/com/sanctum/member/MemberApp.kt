package com.sanctum.member

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.sanctum.member.api.ApiService
import com.sanctum.member.api.RetrofitClient
import com.sanctum.member.data.local.AppDatabase
import com.sanctum.member.models.FcmTokenRequest
import com.sanctum.member.notification.NotificationHelper
import com.sanctum.member.notification.NotificationSyncWorker
import com.sanctum.member.utils.OptimizedTokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MemberApp : Application() {

    lateinit var tokenManager: OptimizedTokenManager
    lateinit var apiService: ApiService
    lateinit var database: AppDatabase
    private lateinit var userPrefs: SharedPreferences

    /** App-scoped coroutine scope for fire-and-forget background calls. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // ── Order matters ─────────────────────────────────────────────────
        // 1. SharedPreferences first (no dependencies)
        userPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // 2. Firebase (no network needed)
        initializeFirebase()

        // 3. TokenManager (reads DataStore — fully async internally)
        tokenManager = OptimizedTokenManager(this)

        // 4. API service (needs tokenManager for auth interceptor)
        apiService = RetrofitClient.create(tokenManager)

        // 5. Room database
        database = AppDatabase.getDatabase(this)

        // 6. Notification channels — must be ready before any FCM arrives
        NotificationHelper.createChannels(this)

        // 7. Schedule periodic notification sync if user is already logged in
        //    (handles app-update restarts where tokenManager cache is preloaded)
        if (tokenManager.isLoggedIn.value) {
            scheduleNotificationSync()
        }
    }

    // ── Firebase ──────────────────────────────────────────────────────────

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
            Log.d(TAG, "Firebase initialised")

            // Fetch + cache FCM token at startup.
            // We intentionally do NOT call sendFcmTokenToServer() here because
            // tokenManager is not yet initialised at this point. The token will
            // be sent by registerFcmTokenWithServer() after login, and by
            // NotificationService.onNewToken() whenever the token rotates.
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    userPrefs.edit().putString("fcm_token", task.result).apply()
                    Log.d(TAG, "FCM token cached locally")
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialisation failed", e)
        }
    }

    // ── FCM token registration ────────────────────────────────────────────

    /**
     * Called by LoginActivity and RegisterActivity after a successful auth.
     * Sends the locally-cached token to the backend and schedules the
     * periodic background sync worker.
     */
    fun onUserLoggedIn() {
        registerFcmTokenWithServer()
        scheduleNotificationSync()
    }

    fun registerFcmTokenWithServer() {
        val cached = userPrefs.getString("fcm_token", null)
        if (cached != null) {
            sendFcmTokenToServer(cached)
        } else {
            // Token not cached yet — fetch fresh then send
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fresh = task.result
                    userPrefs.edit().putString("fcm_token", fresh).apply()
                    sendFcmTokenToServer(fresh)
                } else {
                    Log.e(TAG, "Token fetch failed for server registration", task.exception)
                }
            }
        }
    }

    private fun sendFcmTokenToServer(token: String) {
        appScope.launch {
            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                val response = apiService.registerFcmToken(
                    FcmTokenRequest(token = token, deviceId = deviceId, platform = "android")
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token registered with server")
                } else {
                    Log.w(TAG, "FCM token registration → HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token", e)
            }
        }
    }

    // ── FCM token deregistration (logout) ─────────────────────────────────

    /**
     * Called by ProfileFragment / any logout flow to:
     *  1. Deregister the FCM token from the backend.
     *  2. Cancel the periodic sync worker.
     *  3. Clear user identity from SharedPreferences.
     */
    fun onUserLoggedOut() {
        appScope.launch {
            try {
                apiService.deregisterFcmToken()
                Log.d(TAG, "FCM token deregistered from server")
            } catch (e: Exception) {
                Log.e(TAG, "FCM deregister error (non-fatal)", e)
            }
        }
        WorkManager.getInstance(this).cancelUniqueWork(SYNC_WORK_NAME)
        userPrefs.edit()
            .remove("user_id")
            .remove("church_id")
            .remove("fcm_token")
            .apply()
        Log.d(TAG, "Logout cleanup complete")
    }

    // ── Periodic sync scheduling ──────────────────────────────────────────

    /**
     * Enqueue or update the periodic notification sync worker.
     * Uses [ExistingPeriodicWorkPolicy.UPDATE] so calling this multiple times
     * (e.g. after login, after boot) always produces exactly one chain.
     */
    fun scheduleNotificationSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.d(TAG, "Periodic notification sync scheduled (every 6 h)")
    }

    // ── Shared-prefs helpers ──────────────────────────────────────────────

    fun saveUserInfo(userId: String?, churchId: String?) {
        userPrefs.edit().apply {
            userId?.let    { putString("user_id",   it) }
            churchId?.let  { putString("church_id", it) }
            apply()
        }
    }

    fun getUserId():   String? = userPrefs.getString("user_id",   null)
    fun getChurchId(): String? = userPrefs.getString("church_id", null)

    // ── Companion ─────────────────────────────────────────────────────────

    companion object {
        private const val TAG            = "MemberApp"
        private const val SYNC_WORK_NAME = "notification_sync"

        private lateinit var instance: MemberApp
        fun getInstance(): MemberApp = instance
    }
}
