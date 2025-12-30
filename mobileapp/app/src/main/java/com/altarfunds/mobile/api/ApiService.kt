package com.altarfunds.mobile.api

import com.altarfunds.mobile.BuildConfig
import com.altarfunds.mobile.data.PreferencesManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiService {
    private const val BASE_URL = "https://api.altarfunds.co.ke/api/"
    
    private lateinit var retrofit: Retrofit
    private lateinit var apiInterface: ApiInterface
    
    fun initialize() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            
            // Add auth token if available
            PreferencesManager.authToken?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            // Add device token if available
            PreferencesManager.deviceToken?.let { deviceToken ->
                requestBuilder.addHeader("X-Device-Token", deviceToken)
            }
            
            chain.proceed(requestBuilder.build())
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiInterface = retrofit.create(ApiInterface::class.java)
    }
    
    fun getApiInterface(): ApiInterface {
        if (!::apiInterface.isInitialized) {
            initialize()
        }
        return apiInterface
    }
}
