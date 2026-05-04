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
import com.sanctum.member.utils.TokenManager

class MemberApp : Application() {
    
    lateinit var tokenManager: TokenManager
    lateinit var apiService: ApiService
    lateinit var database: AppDatabase
    private lateinit var userPrefs: SharedPreferences
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase first
        initializeFirebase()
        
        // Initialize SharedPreferences
        userPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        
        // Initialize TokenManager
        tokenManager = TokenManager(this)
        
        // Initialize API Service
        apiService = RetrofitClient.create(tokenManager)
        
        // Initialize Database
        database = AppDatabase.getDatabase(this)
    }
    
    private fun initializeFirebase() {
        try {
            // Initialize Firebase App
            FirebaseApp.initializeApp(this)
            Log.d("MemberApp", "Firebase initialized successfully")
            
            // Get FCM token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("MemberApp", "FCM Token: $token")
                    // Save token locally
                    userPrefs.edit().putString("fcm_token", token).apply()
                    // Send token to server if user is logged in
                    if (tokenManager.isLoggedIn()) {
                        sendTokenToServer(token)
                    }
                } else {
                    Log.e("MemberApp", "Failed to get FCM token", task.exception)
                }
            }
            
            // Configure Firebase Cloud Messaging
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
            Log.d("MemberApp", "FCM auto-init enabled")
            
        } catch (e: Exception) {
            Log.e("MemberApp", "Firebase initialization failed", e)
        }
    }
    
    private fun sendTokenToServer(token: String) {
        // Implementation to send FCM token to your backend
        Log.d("MemberApp", "Sending FCM token to server: $token")
        // This should make an API call to your backend to save the token
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
        
        fun getInstance(): MemberApp {
            return instance
        }
    }
}
