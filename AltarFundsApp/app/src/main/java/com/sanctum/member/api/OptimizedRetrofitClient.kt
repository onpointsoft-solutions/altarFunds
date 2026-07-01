package com.sanctum.member.api

import com.sanctum.member.BuildConfig
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
    
    private var apiService: ApiService? = null
    private val refreshMutex = Mutex()
    
    fun create(tokenManager: OptimizedTokenManager): ApiService {
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
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = tokenManager.getToken()
            
            val requestBuilder = originalRequest.newBuilder()
            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            
            val response = chain.proceed(requestBuilder.build())
            
            if (response.code == 401 && originalRequest.header("X-Token-Refreshed") == null) {
                response.close()
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
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    
    private fun handleTokenRefreshSync(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request,
        tokenManager: OptimizedTokenManager
    ): okhttp3.Response = runBlocking {
        refreshMutex.withLock {
            val currentToken = tokenManager.getToken()
            val requestToken = originalRequest.header("Authorization")?.removePrefix("Bearer ")
            
            // If token already changed, another thread refreshed it
            if (currentToken != null && currentToken != requestToken) {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header("X-Token-Refreshed", "true")
                    .build()
                return@runBlocking chain.proceed(newRequest)
            }
            
            val success = tokenManager.refreshAccessToken()
            val newToken = tokenManager.getToken()
            
            val finalRequest = if (success && newToken != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .header("X-Token-Refreshed", "true")
                    .build()
            } else {
                // Refresh failed, proceed one last time (will likely 401 again)
                originalRequest.newBuilder()
                    .header("X-Token-Refreshed", "true")
                    .build()
            }
            
            chain.proceed(finalRequest)
        }
    }
    
    private class NetworkInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            val newRequest = request.newBuilder()
                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "gzip, deflate")
                .header("User-Agent", "Sanctum-Android/1.0")
                .build()
            return chain.proceed(newRequest)
        }
    }
    
    fun clearCache() {
        apiService = null
    }
}
