package com.sanctum.member.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sanctum.member.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class OptimizedTokenManager(private val context: Context) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val CHURCH_ID_KEY = stringPreferencesKey("church_id")
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
    }
    
    // In-memory cache for instant access
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()
    
    private val _refreshToken = MutableStateFlow<String?>(null)
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()
    
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()
    
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()
    
    private val _churchId = MutableStateFlow<String?>(null)
    val churchId: StateFlow<String?> = _churchId.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        // Preload tokens from DataStore to memory cache
        preloadTokens()
    }
    
    private fun preloadTokens() {
        scope.launch {
            try {
                // Load all auth data at once for better performance
                context.dataStore.data.map { preferences ->
                    _accessToken.value = preferences[ACCESS_TOKEN_KEY]
                    _refreshToken.value = preferences[REFRESH_TOKEN_KEY]
                    _userEmail.value = preferences[USER_EMAIL_KEY]
                    _userId.value = preferences[USER_ID_KEY]
                    _churchId.value = preferences[CHURCH_ID_KEY]
                    _isLoggedIn.value = preferences[ACCESS_TOKEN_KEY] != null
                }.first()
            } catch (e: Exception) {
                // Handle DataStore errors gracefully
                clearCache()
            }
        }
    }
    
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        withContext(Dispatchers.IO) {
            try {
                // Update memory cache immediately
                _accessToken.value = accessToken
                _refreshToken.value = refreshToken
                _isLoggedIn.value = true
                
                // Persist to DataStore asynchronously
                context.dataStore.edit { preferences ->
                    preferences[ACCESS_TOKEN_KEY] = accessToken
                    preferences[REFRESH_TOKEN_KEY] = refreshToken
                }
            } catch (e: Exception) {
                Log.e("OptimizedTokenManager", "Error saving tokens", e)
            }
        }
    }
    
    suspend fun saveUserInfo(userId: String, email: String, churchId: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                // Update memory cache immediately
                _userId.value = userId
                _userEmail.value = email
                _churchId.value = churchId
                
                // Persist to DataStore asynchronously
                context.dataStore.edit { preferences ->
                    preferences[USER_ID_KEY] = userId
                    preferences[USER_EMAIL_KEY] = email
                    churchId?.let { preferences[CHURCH_ID_KEY] = it }
                }
            } catch (e: Exception) {
                Log.e("OptimizedTokenManager", "Error saving user info", e)
            }
        }
    }
    
    suspend fun saveFCMToken(fcmToken: String) {
        withContext(Dispatchers.IO) {
            try {
                context.dataStore.edit { preferences ->
                    preferences[FCM_TOKEN_KEY] = fcmToken
                }
            } catch (e: Exception) {
                Log.e("OptimizedTokenManager", "Error saving FCM token", e)
            }
        }
    }
    
    // Instant access methods - no blocking!
    fun getToken(): String? = _accessToken.value
    fun getRefreshToken(): String? = _refreshToken.value
    fun getUserEmail(): String? = _userEmail.value
    fun getUserId(): String? = _userId.value
    fun getChurchId(): String? = _churchId.value
    
    // Flow-based access for reactive UI
    fun getTokenFlow(): Flow<String?> = accessToken
    fun getUserEmailFlow(): Flow<String?> = userEmail
    fun getChurchIdFlow(): Flow<String?> = churchId
    fun getIsLoggedInFlow(): Flow<Boolean> = isLoggedIn
    
    suspend fun clearTokens() {
        withContext(Dispatchers.IO) {
            try {
                // Clear memory cache immediately
                clearCache()
                
                // Clear DataStore
                context.dataStore.edit { preferences ->
                    preferences.clear()
                }
            } catch (e: Exception) {
                Log.e("OptimizedTokenManager", "Error clearing tokens", e)
            }
        }
    }
    
    private fun clearCache() {
        _accessToken.value = null
        _refreshToken.value = null
        _userEmail.value = null
        _userId.value = null
        _churchId.value = null
        _isLoggedIn.value = false
    }
    
    // Refresh token management
    suspend fun refreshAccessToken(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = _refreshToken.value
                if (refreshToken == null) {
                    clearTokens()
                    return@withContext false
                }
                
                // Call refresh token API
                val apiService = com.sanctum.member.api.OptimizedRetrofitClient.create(this@OptimizedTokenManager)
                val response = apiService.refreshToken(
                    com.sanctum.member.models.RefreshTokenRequest(refreshToken)
                )
                
                if (response.isSuccessful) {
                    val newTokens = response.body()
                    newTokens?.let {
                        saveTokens(it.access, refreshToken)
                        return@withContext true
                    } ?: return@withContext false
                } else {
                    // Refresh token is invalid, clear everything
                    clearTokens()
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("OptimizedTokenManager", "Error refreshing token", e)
                return@withContext false
            }
        }
    }
    
    // Check if token needs refresh (optional: implement token expiration checking)
    fun isTokenExpired(): Boolean {
        // TODO: Parse JWT token and check expiration
        // For now, return false and rely on 401 responses
        return false
    }
}
