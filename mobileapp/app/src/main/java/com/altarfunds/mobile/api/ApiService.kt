package com.altarfunds.mobile.api

import com.altarfunds.mobile.AltarFundsApp
import com.altarfunds.mobile.data.PreferencesManager
import com.altarfunds.mobile.models.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiService {
    // Update to match web backend URL
    //private const val BASE_URL = "http://10.0.2.2:8000/api/"  // For Android emulator
    // private const val BASE_URL = "http://127.0.0.1:8000/api/"  // For local testing
    private const val BASE_URL = "https://altarfunds.pythonanywhere.com/api/"  // Production URL
    
    private lateinit var retrofit: Retrofit
    private lateinit var apiInterface: ApiInterface
    private var preferencesManager: PreferencesManager? = null
    
    fun initialize(app: AltarFundsApp? = null) {
        preferencesManager = app?.preferencesManager
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            
            // Add auth token if available
            preferencesManager?.authToken?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            // Add device token if available
            preferencesManager?.deviceToken?.let { deviceToken ->
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
            initialize(null)
        }
        return apiInterface
    }

    // Convenience methods for web backend endpoints
    suspend fun getFinancialSummary(): FinancialSummary {
        return try {
            val response = getApiInterface().getFinancialSummary()
            if (response.isSuccessful) {
                response.body() ?: FinancialSummary(
                    totalIncome = 0.0,
                    totalExpenses = 0.0,
                    netIncome = 0.0,
                    totalDonations = 0.0,
                    budgetUtilization = 0.0,
                    period = "Current"
                )
            } else {
                FinancialSummary(
                    totalIncome = 0.0,
                    totalExpenses = 0.0,
                    netIncome = 0.0,
                    totalDonations = 0.0,
                    budgetUtilization = 0.0,
                    period = "Current"
                )
            }
        } catch (e: Exception) {
            // Return empty summary on error
            FinancialSummary(
                totalIncome = 0.0,
                totalExpenses = 0.0,
                netIncome = 0.0,
                totalDonations = 0.0,
                budgetUtilization = 0.0,
                period = "Current"
            )
        }
    }

    suspend fun getRecentTransactions(limit: Int = 5): List<GivingTransactionResponse> {
        return try {
            val response = getApiInterface().getDonations(limit = limit)
            if (response.isSuccessful) {
                response.body()?.donations?.map { donation ->
                    GivingTransactionResponse(
                        transaction_id = donation.id,
                        amount = donation.amount,
                        category = GivingCategory(id = 1, name = donation.fundType ?: "general", description = donation.fundType, is_tax_deductible = true, is_active = true),
                        status = "completed",
                        payment_method = donation.paymentMethod ?: "unknown",
                        transaction_date = donation.date,
                        note = donation.reference
                    )
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserProfile(): User {
        return try {
            val response = getApiInterface().getCurrentUser()
            if (response.isSuccessful) {
                val userResponse = response.body()
                User(
                    id = userResponse?.id ?: "",
                    email = userResponse?.email ?: "",
                    first_name = userResponse?.first_name ?: "User",
                    last_name = userResponse?.last_name ?: "",
                    phone_number = userResponse?.phone_number,
                    role = userResponse?.role ?: "member",
                    is_active = true
                )
            } else {
                User("", "user@example.com", "User", "", null, "member", true)
            }
        } catch (e: Exception) {
            User("", "user@example.com", "User", "", null, "member", true)
        }
    }

    suspend fun getBudgets(): List<Budget> {
        return try {
            val response = getApiInterface().getBudgets()
            if (response.isSuccessful) {
                response.body()?.budgets ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getExpenses(): List<Expense> {
        return try {
            val response = getApiInterface().getExpenses()
            if (response.isSuccessful) {
                response.body()?.expenses ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
