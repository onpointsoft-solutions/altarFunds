package com.altarfunds.mobile.data.api

import com.altarfunds.mobile.data.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiClient {
    
    private val baseUrl = "https://altarfunds.pythonanywhere.com/api" // For Android emulator
    // Use "http://localhost:8000/api" for physical device on same network
    
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            
            // Add auth token if available
            getAuthToken()?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }
            
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()
    
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    // Auth token management
    private var authToken: String? = null
    
    fun setAuthToken(token: String) {
        authToken = token
    }
    
    fun getAuthToken(): String? = authToken
    
    fun clearAuthToken() {
        authToken = null
    }
    
    // Generic request methods
    private suspend fun <T> get(endpoint: String, responseType: TypeToken<T>): T {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl$endpoint")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Request failed: ${response.code} ${response.message}")
            }
            
            val responseBody = response.body?.string() 
                ?: throw IOException("Empty response body")
            
            gson.fromJson(responseBody, responseType.type)
        }
    }
    
    private suspend fun <T> post(endpoint: String, body: Any?, responseType: TypeToken<T>): T {
        return withContext(Dispatchers.IO) {
            val jsonBody = gson.toJson(body)
            val requestBody = jsonBody.toRequestBody(jsonMediaType)
            
            val request = Request.Builder()
                .url("$baseUrl$endpoint")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Request failed: ${response.code} ${response.message}")
            }
            
            val responseBody = response.body?.string() 
                ?: throw IOException("Empty response body")
            
            gson.fromJson(responseBody, responseType.type)
        }
    }
    
    private suspend fun delete(endpoint: String) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl$endpoint")
                .delete()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("Request failed: ${response.code} ${response.message}")
            }
        }
    }
    
    // User endpoints
    suspend fun getCurrentUser(): User {
        return get("/accounts/profile/", object : TypeToken<User>() {})
    }
    
    suspend fun logout() {
        post<Unit>("/accounts/logout/", null, object : TypeToken<Unit>() {})
        clearAuthToken()
    }
    
    // Devotionals endpoints
    suspend fun getDevotionals(): List<Devotional> {
        return get("/devotionals/", object : TypeToken<List<Devotional>>() {})
    }
    
    suspend fun getDevotional(id: Int): Devotional {
        return get("/devotionals/$id/", object : TypeToken<Devotional>() {})
    }
    
    suspend fun reactToDevotional(devotionalId: Int, reactionType: String) {
        val body = mapOf("reaction_type" to reactionType)
        post<Unit>("/devotionals/$devotionalId/react/", body, object : TypeToken<Unit>() {})
    }
    
    suspend fun unreactToDevotional(devotionalId: Int) {
        delete("/devotionals/$devotionalId/unreact/")
    }
    
    suspend fun commentOnDevotional(devotionalId: Int, content: String): DevotionalComment {
        val body = mapOf("content" to content)
        return post("/devotionals/$devotionalId/comment/", body, 
            object : TypeToken<DevotionalComment>() {})
    }
    
    // Notice endpoints (placeholder - implement when backend is ready)
    suspend fun getNotices(): List<Notice> {
        // TODO: Implement when backend notice endpoint is ready
        return emptyList()
    }
    
    suspend fun getNotice(id: Int): Notice {
        // TODO: Implement when backend notice endpoint is ready
        throw NotImplementedError("Notice endpoint not yet implemented")
    }
    
    // Giving/Donations endpoints
    suspend fun getDonations(): List<Any> {
        return get("/donations/", object : TypeToken<List<Any>>() {})
    }
    
    suspend fun createDonation(donation: Map<String, Any>): Any {
        return post("/donations/", donation, object : TypeToken<Any>() {})
    }
}
