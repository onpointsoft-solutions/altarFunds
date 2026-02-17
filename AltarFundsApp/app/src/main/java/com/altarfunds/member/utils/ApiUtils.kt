package com.altarfunds.member.utils

import com.altarfunds.member.MemberApp
import com.altarfunds.member.models.RefreshTokenRequest
import kotlinx.coroutines.runBlocking

object ApiUtils {
    
    /**
     * Execute API call with automatic token refresh on 401 errors
     */
    suspend fun <T> executeWithRefresh(
        apiCall: suspend () -> retrofit2.Response<T>
    ): retrofit2.Response<T> {
        val app = MemberApp.getInstance()
        
        // First attempt
        val response = apiCall()
        
        // If 401 (unauthorized), try to refresh token and retry once
        if (response.code() == 401) {
            try {
                val refreshToken = app.tokenManager.getRefreshToken()
                if (!refreshToken.isNullOrEmpty()) {
                    val refreshResponse = app.apiService.refreshToken(RefreshTokenRequest(refreshToken))
                    
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newToken = refreshResponse.body()!!.access
                        // Save both tokens since saveTokens expects both access and refresh
                        app.tokenManager.saveTokens(newToken, refreshToken)
                        
                        // Retry the original call with new token
                        return apiCall()
                    }
                }
            } catch (e: Exception) {
                // Refresh failed, continue with original error
            }
        }
        
        return response
    }
}
