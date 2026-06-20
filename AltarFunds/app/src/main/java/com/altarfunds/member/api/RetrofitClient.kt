package com.altarfunds.member.api

import android.content.Context
import android.content.Intent
import com.altarfunds.member.models.RefreshTokenRequest
import com.altarfunds.member.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://backend.sanctum.co.ke/api/"

    fun create(tokenManager: TokenManager, context: Context): ApiService {

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // ── Auth interceptor with silent token refresh ─────────────────────
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = tokenManager.getToken()

            // Attach current token (or proceed without if not logged in)
            val authenticatedRequest = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }

            val response = chain.proceed(authenticatedRequest)

            // On 401: try one silent refresh then retry, otherwise force logout
            if (response.code == 401 && token != null) {
                response.close()

                val refreshed = runBlocking {
                    val refreshToken = tokenManager.getRefreshToken() ?: return@runBlocking false
                    try {
                        // Use a plain OkHttpClient (no auth interceptor) for the refresh call
                        val refreshClient = OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build()
                        val refreshRetrofit = Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(refreshClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                        val refreshApi = refreshRetrofit.create(ApiService::class.java)
                        val refreshResponse = refreshApi.refreshToken(RefreshTokenRequest(refreshToken))
                        if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                            tokenManager.saveTokens(
                                refreshResponse.body()!!.access,
                                refreshToken               // keep the same refresh token
                            )
                            true
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                }

                if (refreshed) {
                    // Retry the original request with the new access token
                    val newToken = tokenManager.getToken()
                    val retryRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    chain.proceed(retryRequest)
                } else {
                    // Refresh failed — clear session and redirect to login
                    runBlocking { tokenManager.clearTokens() }
                    redirectToLogin(context)
                    response
                }
            } else {
                response
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun redirectToLogin(context: Context) {
        val intent = Intent(context, com.altarfunds.member.ui.auth.LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
