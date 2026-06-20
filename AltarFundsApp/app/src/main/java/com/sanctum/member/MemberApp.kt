package com.sanctum.member

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.sanctum.member.api.ApiService
import com.sanctum.member.api.RetrofitClient
import com.sanctum.member.data.local.AppDatabase
import com.sanctum.member.models.FcmTokenRequest
import com.sanctum.member.notification.NotificationHelper
import com.sanctum.member.utils.OptimizedTokenManager
import com.sanctum.member.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MemberApp : Application() {
    
    lateinit var tokenManager: OptimizedTokenManager
    lateinit var apiService: ApiService
    lateinit var database: AppDatabase
    private lateinit var userPrefs: SharedPreferences
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase first
        initializeFirebase()
        
        // Initialize SharedPreferences
        userPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        
        // Initialize TokenManager
        tokenManager = OptimizedTokenManager(this)
        
        // Initialize API Service
        apiService = RetrofitClient.create(tokenManager)
        
        // Initialize Database
        database = AppDatabase.getDatabase(this)

        // Create notification channels at startup so they are ready before
        // any FCM message arrives (required on Android O+)
        NotificationHelper.createChannels(this)
    }
    
    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MemberApp", "Firebase initialized successfully")
            
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("MemberApp", "FCM Token obtained")
                    // Persist token locally
                    getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        .edit().putString("fcm_token", token).apply()
                    // Send to server if user is already logged in
                    if (tokenManager.isLoggedIn.value) {
                        sendFcmTokenToServer(token)
                    }
                } else {
                    Log.e("MemberApp", "Failed to get FCM token", task.exception)
                }
            }
            
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
            Log.d("MemberApp", "FCM auto-init enabled")
            
        } catch (e: Exception) {
            Log.e("MemberApp", "Firebase initialization failed", e)
        }
    }

    /**
     * Called by LoginActivity after a successful login to register the
     * locally-cached FCM token with the backend.
     */
    fun registerFcmTokenWithServer() {
        val token = userPrefs.getString("fcm_token", null) ?: run {
            // Token not cached yet — fetch it now then register
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val freshToken = task.result
                    userPrefs.edit().putString("fcm_token", freshToken).apply()
                    sendFcmTokenToServer(freshToken)
                }
            }
            return
        }
        sendFcmTokenToServer(token)
    }

    private fun sendFcmTokenToServer(token: String) {
        appScope.launch {
            try {
                // Include device ID so the backend can do per-device dedup
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                val response = apiService.registerFcmToken(
                    FcmTokenRequest(token = token, deviceId = deviceId, platform = "android")
                )
                if (response.isSuccessful) {
                    Log.d("MemberApp", "FCM token registered with server successfully")
                } else {
                    Log.w("MemberApp", "FCM token registration returned ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("MemberApp", "Failed to register FCM token with server", e)
            }
        }
    }

    fun saveUserInfo(userId: String?, churchId: String?) {
        userPrefs.edit().apply {
            userId?.let { putString("user_id", it) }
            churchId?.let { putString("church_id", it) }
            apply()
        }
    }
    
    fun getUserId(): String? = userPrefs.getString("user_id", null)
    fun getChurchId(): String? = userPrefs.getString("church_id", null)
    
    companion object {
        private lateinit var instance: MemberApp
        
        fun getInstance(): MemberApp = instance
    }
}
