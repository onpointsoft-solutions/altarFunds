package com.sanctum.member.api

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.sanctum.member.BuildConfig
import com.sanctum.member.models.GivingCategoryRef
import com.sanctum.member.models.RefreshTokenRequest
import com.sanctum.member.utils.OptimizedTokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Custom deserializer for GivingCategoryRef.
 *
 * The mobile list endpoint returns category as a bare integer (e.g. 7),
 * while the detail endpoint may return a full JSON object.
 * This deserializer handles both shapes.
 */
private class GivingCategoryRefDeserializer : JsonDeserializer<GivingCategoryRef> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): GivingCategoryRef {
        return when {
            json.isJsonPrimitive -> {
                // Bare integer — just store the ID; name unknown from this endpoint
                GivingCategoryRef(id = json.asInt, name = "")
            }
            json.isJsonObject -> {
                val obj = json.asJsonObject
                GivingCategoryRef(
                    id   = obj.get("id")?.asInt ?: 0,
                    name = obj.get("name")?.asString ?: "",
                    description = obj.get("description")?.asString,
                )
            }
            else -> GivingCategoryRef()
        }
    }
}

/**
 * Retrofit client with:
 *  - Bearer token injection on every request
 *  - Silent JWT refresh on 401 via OkHttp Authenticator (NOT runBlocking inside Interceptor)
 *  - Redirect to LoginActivity after failed refresh
 *  - BODY logging in debug, NONE in release
 *  - Base URL driven by BuildConfig so it can be switched without a rebuild
 */
object RetrofitClient {

    // Switch environments by setting BASE_URL in build.gradle.kts:
    //   buildConfigField("String", "BASE_URL", '"https://backend.sanctum.co.ke/api/"')
    // Falls back to the hardcoded value if the field is not set.
    private fun baseUrl(): String = BuildConfig.BASE_URL

    fun create(tokenManager: OptimizedTokenManager): ApiService {

        // ── 1. Bearer token injection ─────────────────────────────────────
        val authHeaderInterceptor = Interceptor { chain ->
            val token = tokenManager.getToken()
            val req = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(req)
        }

        // ── 2. Silent refresh on 401 — OkHttp Authenticator ──────────────
        // OkHttp calls authenticate() on a background IO thread, so runBlocking
        // here does NOT cause a deadlock (unlike inside a regular Interceptor).
        val tokenAuthenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
                // Avoid infinite retry: if the retry request also fails with 401, stop.
                if (response.request.header("X-Retry-Auth") != null) {
                    Log.w("RetrofitClient", "Retry request also got 401 — clearing session")
                    runBlocking { tokenManager.clearTokens() }
                    redirectToLogin()
                    return null
                }

                Log.d("RetrofitClient", "401 received — attempting silent token refresh")

                val refreshed = runBlocking { tokenManager.refreshAccessToken() }

                return if (refreshed) {
                    val newToken = tokenManager.getToken()
                    Log.d("RetrofitClient", "Token refreshed — retrying original request")
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .header("X-Retry-Auth", "1")   // prevent infinite loop
                        .build()
                } else {
                    Log.w("RetrofitClient", "Token refresh failed — redirecting to login")
                    redirectToLogin()
                    null  // returning null cancels the request
                }
            }
        }

        // ── 3. Logging — BODY in debug, NONE in release ───────────────────
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authHeaderInterceptor)  // attaches token before the request
            .authenticator(tokenAuthenticator)       // handles 401 after the response
            .addInterceptor(loggingInterceptor)      // log last so we see final headers
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .registerTypeAdapter(GivingCategoryRef::class.java, GivingCategoryRefDeserializer())
                        .create()
                )
            )
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Fire an intent to LoginActivity from a background thread.
     * Safe because FLAG_ACTIVITY_NEW_TASK is set.
     */
    private fun redirectToLogin() {
        try {
            val app = com.sanctum.member.MemberApp.getInstance()
            val intent = android.content.Intent(
                app,
                com.sanctum.member.ui.auth.LoginActivity::class.java
            ).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            app.startActivity(intent)
        } catch (e: Exception) {
            Log.e("RetrofitClient", "Failed to redirect to login", e)
        }
    }
}
