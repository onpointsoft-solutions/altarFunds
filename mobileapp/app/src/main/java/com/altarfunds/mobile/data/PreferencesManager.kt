package com.altarfunds.mobile.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

class PreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Initialize preferences on creation
    
    companion object {
        private const val PREFS_NAME = "altar_funds_prefs"
        
        // Keys
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_CHURCH_ID = "church_id"
        private const val KEY_CHURCH_NAME = "church_name"
        private const val KEY_MEMBER_ID = "member_id"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_APP_VERSION = "app_version"
        
        // Settings keys
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_EMAIL_NOTIFICATIONS_ENABLED = "email_notifications_enabled"
        private const val KEY_PUSH_NOTIFICATIONS_ENABLED = "push_notifications_enabled"
        private const val KEY_SAVE_PAYMENT_METHOD = "save_payment_method"
        private const val KEY_BIOMETRIC_AUTH = "biometric_auth"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_CURRENT_CHURCH = "current_church"
    }
    
    var authToken: String?
        get() = sharedPreferences.getString(KEY_AUTH_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_AUTH_TOKEN, value).apply()
    
    var refreshToken: String?
        get() = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, value).apply()
    
    var userEmail: String?
        get() = sharedPreferences.getString(KEY_USER_EMAIL, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_EMAIL, value).apply()
    
    var userName: String?
        get() = sharedPreferences.getString(KEY_USER_NAME, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_NAME, value).apply()
    
    var userId: String?
        get() = sharedPreferences.getString(KEY_USER_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_ID, value).apply()
    
    var deviceToken: String?
        get() = sharedPreferences.getString(KEY_DEVICE_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_DEVICE_TOKEN, value).apply()
    
    var churchId: String?
        get() = sharedPreferences.getString(KEY_CHURCH_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_CHURCH_ID, value).apply()
    
    var churchName: String?
        get() = sharedPreferences.getString(KEY_CHURCH_NAME, null)
        set(value) = sharedPreferences.edit().putString(KEY_CHURCH_NAME, value).apply()
    
    var memberId: String?
        get() = sharedPreferences.getString(KEY_MEMBER_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_MEMBER_ID, value).apply()
    
    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, value).apply()
    
    var lastSync: Long
        get() = sharedPreferences.getLong(KEY_LAST_SYNC, 0)
        set(value) = sharedPreferences.edit().putLong(KEY_LAST_SYNC, value).apply()
    
    var appVersion: String?
        get() = sharedPreferences.getString(KEY_APP_VERSION, null)
        set(value) = sharedPreferences.edit().putString(KEY_APP_VERSION, value).apply()
    
    // Core Settings
    fun getNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
    fun setNotificationsEnabled(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
    }
    fun getEmailNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_EMAIL_NOTIFICATIONS_ENABLED, true)
    }
    fun setEmailNotificationsEnabled(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_EMAIL_NOTIFICATIONS_ENABLED, value).apply()
    }
    fun getPushNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_PUSH_NOTIFICATIONS_ENABLED, true)
    }
    fun setPushNotificationsEnabled(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PUSH_NOTIFICATIONS_ENABLED, value).apply()
    }
    fun getSavePaymentMethodEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SAVE_PAYMENT_METHOD, false)
    }
    fun setSavePaymentMethodEnabled(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SAVE_PAYMENT_METHOD, value).apply()
    }

    fun getBiometricAuthEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_AUTH, false)
    }
    fun setBiometricAuthEnabled(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_AUTH, value).apply()
    }
    fun getLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, "English") ?: "English"
    }

    fun setLanguage(value: String) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, value).apply()
    }

    fun getCurrency(): String {
        return sharedPreferences.getString(KEY_CURRENCY, "KES") ?: "KES"
    }

    fun setCurrency(value: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY, value).apply()
    }
    
    fun getCurrentChurch(): com.altarfunds.mobile.models.ChurchInfo? {
        val churchJson = sharedPreferences.getString(KEY_CURRENT_CHURCH, null)
        return if (churchJson != null) {
            gson.fromJson(churchJson, com.altarfunds.mobile.models.ChurchInfo::class.java)
        } else null
    }
    
    fun setCurrentChurch(church: com.altarfunds.mobile.models.ChurchInfo) {
        val churchJson = gson.toJson(church)
        sharedPreferences.edit().putString(KEY_CURRENT_CHURCH, churchJson).apply()
    }
    
    fun clearUserData() {
        sharedPreferences.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ID)
            remove(KEY_CHURCH_ID)
            remove(KEY_CHURCH_NAME)
            remove(KEY_MEMBER_ID)
        }.apply()
    }
    
    fun saveUserSession(loginResponse: com.altarfunds.mobile.models.LoginResponse) {
        authToken = loginResponse.access_token
        refreshToken = loginResponse.refresh_token
        userId = loginResponse.user.id
        userEmail = loginResponse.user.email
        userName = "${loginResponse.user.first_name} ${loginResponse.user.last_name}"
        
        loginResponse.member?.let { member ->
            memberId = member.id
        }
        
        deviceToken = loginResponse.device.device_token
    }
    
    fun isLoggedIn(): Boolean {
        return !authToken.isNullOrEmpty()
    }
    
    // Test method to verify class is working
    fun testMethod(): String {
        return "PreferencesManager is working"
    }
    
    // Additional preference methods for comprehensive settings
    var darkModeEnabled: Boolean
        get() = sharedPreferences.getBoolean("dark_mode_enabled", false)
        set(value) = sharedPreferences.edit().putBoolean("dark_mode_enabled", value).apply()
    
    var autoBackupEnabled: Boolean
        get() = sharedPreferences.getBoolean("auto_backup_enabled", false)
        set(value) = sharedPreferences.edit().putBoolean("auto_backup_enabled", value).apply()
    
    var fontSize: Float
        get() = sharedPreferences.getFloat("font_size", 14.0f)
        set(value) = sharedPreferences.edit().putFloat("font_size", value).apply()
    
    var dataUsageTracking: Boolean
        get() = sharedPreferences.getBoolean("data_usage_tracking", true)
        set(value) = sharedPreferences.edit().putBoolean("data_usage_tracking", value).apply()
    
    fun getSettingValue(key: String, defaultValue: String): String? {
        return sharedPreferences.getString(key, defaultValue)
    }
    
    fun setSettingValue(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
    
    fun getSettingBool(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
    
    fun setSettingBool(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
}
