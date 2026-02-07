package com.altarfunds.member.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }
    
    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
    
    suspend fun saveUserInfo(userId: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
        }
    }
    
    fun getToken(): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[ACCESS_TOKEN_KEY]
            }.first()
        }
    }
    
    fun getRefreshToken(): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[REFRESH_TOKEN_KEY]
            }.first()
        }
    }
    
    fun getUserEmail(): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[USER_EMAIL_KEY]
            }.first()
        }
    }
    
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
    
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }
}
