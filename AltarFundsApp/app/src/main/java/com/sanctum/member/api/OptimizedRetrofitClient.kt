package com.sanctum.member.api

import com.sanctum.member.utils.OptimizedTokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object OptimizedRetrofitClient {
    
    private const val BASE_URL = "https://backend.sanctum.co.ke/api/"
    private var apiService: ApiService? = null
    private val isRefreshing = java.util.concurrent.atomic.AtomicBoolean(false)
    
    fun create(tokenManager: OptimizedTokenManager): ApiService {
        // Cache the API service instance
        if (apiService == null) {
            synchronized(this) {
                if (apiService == null) {
                    apiService = buildApiService(tokenManager)
                }
            }
        }
        return apiService!!
    }
    
    private fun buildApiService(tokenManager: OptimizedTokenManager): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            
            // Get current token from StateFlow (instant access!)
            val token = tokenManager.getToken()
            
            val newRequest = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
            
            val response = chain.proceed(newRequest)
            
            // Handle 401 Unauthorized with automatic token refresh
            if (response.code == 401) {
                return@Interceptor handleTokenRefreshSync(chain, originalRequest, tokenManager)
            }
            
            response
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(NetworkInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    
    private fun handleTokenRefreshSync(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request,
        tokenManager: OptimizedTokenManager
    ): okhttp3.Response {
        
        // Use atomic flag to prevent multiple simultaneous refresh attempts
        if (!isRefreshing.compareAndSet(false, true)) {
            // Another thread is already refreshing, just proceed with original request
            return chain.proceed(originalRequest)
        }
        
        return try {
            // Try to refresh the token synchronously
            val refreshSuccess = runBlocking {
                tokenManager.refreshAccessToken()
            }
            
            if (refreshSuccess) {
                // Retry the original request with new token
                val newToken = tokenManager.getToken()
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                chain.proceed(newRequest)
            } else {
                // Token refresh failed, clear tokens and let UI handle logout
                runBlocking {
                    tokenManager.clearTokens()
                }
                chain.proceed(originalRequest)
            }
        } catch (e: Exception) {
            // Error during refresh, clear tokens
            runBlocking {
                tokenManager.clearTokens()
            }
            chain.proceed(originalRequest)
        } finally {
            // Reset the refresh flag
            isRefreshing.set(false)
        }
    }
    
        
    // Network interceptor for better connectivity handling
    private class NetworkInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            
            // Add network-related headers
            val newRequest = request.newBuilder()
                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "gzip, deflate")
                .header("User-Agent", "Sanctum-Android/1.0")
                .build()
            
            return chain.proceed(newRequest)
        }
    }
    
    // Clear cached service (useful for logout)
    fun clearCache() {
        apiService = null
    }
}
