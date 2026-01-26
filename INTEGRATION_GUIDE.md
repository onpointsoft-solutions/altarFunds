# AltarFunds - Complete Integration Guide

## 🎯 Overview

This guide provides complete integration instructions for connecting the mobile app and web app to the Django backend.

---

## 📱 Mobile App Integration (Android)

### Step 1: Update build.gradle

Add Paystack SDK and update dependencies:

```gradle
// app/build.gradle
dependencies {
    // Existing dependencies
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    
    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    
    // Paystack
    implementation 'co.paystack.android:paystack:3.1.3'
}
```

### Step 2: Update API Interface

Update `ApiInterface.kt` with all new endpoints:

```kotlin
package com.altarfunds.mobile.api

import com.altarfunds.mobile.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiInterface {
    
    // ============ Authentication ============
    @POST("auth/token/")
    suspend fun login(@Body credentials: LoginRequest): Response<LoginResponse>
    
    @POST("auth/token/refresh/")
    suspend fun refreshToken(@Body refresh: RefreshTokenRequest): Response<TokenResponse>
    
    @GET("accounts/profile/")
    suspend fun getUserProfile(): Response<ApiResponse<User>>
    
    @PUT("accounts/profile/")
    suspend fun updateProfile(@Body profile: ProfileUpdateRequest): Response<ApiResponse<User>>
    
    // ============ Churches ============
    @GET("churches/")
    suspend fun getChurches(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null
    ): Response<ApiResponse<PaginatedResponse<Church>>>
    
    @GET("churches/{id}/")
    suspend fun getChurchDetails(@Path("id") churchId: Int): Response<ApiResponse<Church>>
    
    @POST("churches/")
    suspend fun createChurch(@Body church: ChurchCreateRequest): Response<ApiResponse<Church>>
    
    @POST("churches/{id}/join/")
    suspend fun joinChurch(@Path("id") churchId: Int): Response<ApiResponse<JoinChurchResponse>>
    
    @POST("churches/transfer/")
    suspend fun transferChurch(@Body transfer: ChurchTransferRequest): Response<ApiResponse<TransferResponse>>
    
    @GET("churches/{id}/members/")
    suspend fun getChurchMembers(@Path("id") churchId: Int): Response<ApiResponse<MembersResponse>>
    
    @GET("churches/pending-approval/")
    suspend fun getPendingChurches(): Response<ApiResponse<List<Church>>>
    
    @POST("churches/{id}/approve/")
    suspend fun approveChurch(@Path("id") churchId: Int): Response<ApiResponse<ApprovalResponse>>
    
    @POST("churches/{id}/reject/")
    suspend fun rejectChurch(
        @Path("id") churchId: Int,
        @Body reason: RejectionRequest
    ): Response<ApiResponse<ApprovalResponse>>
    
    // ============ Giving ============
    @GET("giving/transactions/")
    suspend fun getGivings(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("giving_type") givingType: String? = null
    ): Response<ApiResponse<PaginatedResponse<GivingTransaction>>>
    
    @POST("giving/transactions/")
    suspend fun createGiving(@Body giving: GivingCreateRequest): Response<ApiResponse<GivingTransaction>>
    
    @GET("giving/transactions/history/")
    suspend fun getGivingHistory(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<ApiResponse<GivingHistoryResponse>>
    
    @GET("giving/transactions/summary/")
    suspend fun getGivingSummary(): Response<ApiResponse<GivingSummaryResponse>>
    
    @GET("giving/church/{id}/")
    suspend fun getChurchGivings(@Path("id") churchId: Int): Response<ApiResponse<ChurchGivingsResponse>>
    
    @GET("giving/categories/")
    suspend fun getGivingCategories(): Response<ApiResponse<List<GivingCategory>>>
    
    // ============ Payments ============
    @POST("payments/payments/initialize_paystack/")
    suspend fun initializePaystackPayment(
        @Body payment: PaymentInitRequest
    ): Response<ApiResponse<PaymentInitResponse>>
    
    @GET("payments/payments/verify_payment/")
    suspend fun verifyPayment(@Query("reference") reference: String): Response<ApiResponse<PaymentVerifyResponse>>
    
    @GET("payments/payments/")
    suspend fun getPayments(): Response<ApiResponse<PaginatedResponse<Payment>>>
    
    // ============ Reports ============
    @GET("reports/financial-summary/")
    suspend fun getFinancialSummary(
        @Query("church_id") churchId: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<ApiResponse<FinancialSummary>>
    
    @GET("reports/giving-trends/")
    suspend fun getGivingTrends(
        @Query("period") period: String? = "monthly"
    ): Response<ApiResponse<GivingTrendsResponse>>
    
    @GET("reports/member-statistics/")
    suspend fun getMemberStatistics(
        @Query("church_id") churchId: Int? = null
    ): Response<ApiResponse<MemberStatisticsResponse>>
    
    @GET("reports/church-performance/")
    suspend fun getChurchPerformance(
        @Query("church_id") churchId: Int? = null
    ): Response<ApiResponse<ChurchPerformanceResponse>>
    
    @GET("reports/system-overview/")
    suspend fun getSystemOverview(): Response<ApiResponse<SystemOverviewResponse>>
}

// ============ Request Models ============
data class LoginRequest(val email: String, val password: String)
data class RefreshTokenRequest(val refresh: String)
data class ProfileUpdateRequest(val first_name: String, val last_name: String, val phone_number: String)
data class ChurchCreateRequest(val name: String, val location: String, val address: String)
data class ChurchTransferRequest(val from_church_id: Int, val to_church_id: Int, val reason: String)
data class RejectionRequest(val reason: String)
data class GivingCreateRequest(val amount: Double, val giving_type: String, val church_id: Int, val note: String?)
data class PaymentInitRequest(val amount: Double, val giving_type: String, val church_id: Int, val email: String? = null)

// ============ Response Models ============
data class LoginResponse(val access: String, val refresh: String)
data class TokenResponse(val access: String)
data class ApiResponse<T>(val success: Boolean, val data: T?, val message: String?)
data class PaginatedResponse<T>(val count: Int, val next: String?, val previous: String?, val results: List<T>)
data class JoinChurchResponse(val message: String, val church: Church)
data class TransferResponse(val message: String, val reference: String)
data class MembersResponse(val church: Church, val members: List<Member>, val total_members: Int)
data class ApprovalResponse(val message: String)
data class GivingHistoryResponse(val total_given: Double, val transaction_count: Int, val givings: List<GivingTransaction>)
data class GivingSummaryResponse(val year: Int, val total_given: Double, val transaction_count: Int, val by_category: List<CategorySummary>, val monthly_totals: List<MonthlyTotal>)
data class ChurchGivingsResponse(val total_received: Double, val transaction_count: Int, val by_category: List<CategorySummary>, val recent_givings: List<GivingTransaction>)
data class PaymentInitResponse(val authorization_url: String, val access_code: String, val reference: String)
data class PaymentVerifyResponse(val status: String, val amount: Double, val reference: String, val paid_at: String?)
data class FinancialSummary(val total_income: Double, val total_expenses: Double, val net_income: Double, val budget_utilization: Double)
data class GivingTrendsResponse(val trends: List<TrendData>, val by_type: List<CategorySummary>, val top_givers: List<TopGiver>)
data class MemberStatisticsResponse(val total_members: Int, val active_members: Int, val new_members_this_month: Int, val tithe_payers: Int)
data class ChurchPerformanceResponse(val church: Church, val givings: GivingMetrics, val budget: BudgetMetrics, val expenses: ExpenseMetrics)
data class SystemOverviewResponse(val churches: ChurchStats, val members: MemberStats, val financials: FinancialStats)

data class CategorySummary(val category__name: String, val total: Double, val count: Int)
data class MonthlyTotal(val month: Int, val total: Double, val count: Int)
data class TrendData(val period: String, val total: Double, val count: Int)
data class TopGiver(val name: String, val total: Double, val count: Int)
data class GivingMetrics(val this_month: Double, val last_month: Double, val growth_percentage: Double)
data class BudgetMetrics(val total: Double, val spent: Double, val remaining: Double)
data class ExpenseMetrics(val this_month: Double)
data class ChurchStats(val total: Int, val active: Int, val pending: Int)
data class MemberStats(val total: Int)
data class FinancialStats(val total_givings_this_month: Double, val total_expenses_this_month: Double, val net_income: Double)
```

### Step 3: Create Paystack Payment Handler

Create `PaystackPaymentHandler.kt`:

```kotlin
package com.altarfunds.mobile.payment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.altarfunds.mobile.api.ApiService
import kotlinx.coroutines.*

class PaystackPaymentHandler(private val activity: Activity) {
    
    private var paymentJob: Job? = null
    
    fun initiatePayment(
        amount: Double,
        givingType: String,
        churchId: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        paymentJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Initialize payment with backend
                val response = ApiService.getApiInterface().initializePaystackPayment(
                    PaymentInitRequest(
                        amount = amount,
                        giving_type = givingType,
                        church_id = churchId
                    )
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val authUrl = data?.authorization_url
                    val reference = data?.reference
                    
                    if (authUrl != null && reference != null) {
                        // Open Paystack checkout in browser
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                        activity.startActivity(intent)
                        
                        // Start polling for payment verification
                        pollPaymentStatus(reference, onSuccess, onError)
                    } else {
                        onError("Invalid payment response")
                    }
                } else {
                    onError(response.body()?.message ?: "Payment initialization failed")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Network error")
            }
        }
    }
    
    private fun pollPaymentStatus(
        reference: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            var attempts = 0
            val maxAttempts = 30 // 5 minutes (10 seconds * 30)
            
            while (attempts < maxAttempts) {
                delay(10000) // Wait 10 seconds
                
                try {
                    val response = ApiService.getApiInterface().verifyPayment(reference)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val status = response.body()?.data?.status
                        
                        when (status) {
                            "success" -> {
                                onSuccess(reference)
                                return@launch
                            }
                            "failed" -> {
                                onError("Payment failed")
                                return@launch
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continue polling
                }
                
                attempts++
            }
            
            onError("Payment verification timeout. Please check your giving history.")
        }
    }
    
    fun cancelPayment() {
        paymentJob?.cancel()
    }
}
```

### Step 4: Update GivingActivity

Update your `GivingActivity.kt` or `NewGivingActivity.kt`:

```kotlin
package com.altarfunds.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.mobile.databinding.ActivityNewGivingBinding
import com.altarfunds.mobile.payment.PaystackPaymentHandler
import kotlinx.coroutines.launch

class NewGivingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNewGivingBinding
    private lateinit var paymentHandler: PaystackPaymentHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewGivingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        paymentHandler = PaystackPaymentHandler(this)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnSubmit.setOnClickListener {
            processPayment()
        }
    }
    
    private fun processPayment() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val givingType = binding.spinnerGivingType.selectedItem.toString()
        val churchId = getChurchId() // Get from preferences
        
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading
        binding.btnSubmit.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        paymentHandler.initiatePayment(
            amount = amount,
            givingType = givingType,
            churchId = churchId,
            onSuccess = { reference ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Payment successful! Reference: $reference",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            },
            onError = { error ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(
                        this,
                        "Payment failed: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
    
    private fun getChurchId(): Int {
        // Get from SharedPreferences or user profile
        return 1 // Replace with actual church ID
    }
    
    override fun onDestroy() {
        super.onDestroy()
        paymentHandler.cancelPayment()
    }
}
```

---

## 🌐 Web App Integration

### Step 1: Create API Helper Library

Create `web/assets/js/api.js`:

```javascript
// API Helper Library for AltarFunds Web App
const API_BASE_URL = 'https://altarfunds.pythonanywhere.com/api';

class AltarFundsAPI {
    constructor() {
        this.baseURL = API_BASE_URL;
    }
    
    // Get auth token from localStorage
    getToken() {
        return localStorage.getItem('access_token');
    }
    
    // Set auth token
    setToken(token) {
        localStorage.setItem('access_token', token);
    }
    
    // Remove auth token
    removeToken() {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
    }
    
    // Make authenticated request
    async request(endpoint, options = {}) {
        const token = this.getToken();
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };
        
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        
        const response = await fetch(`${this.baseURL}${endpoint}`, {
            ...options,
            headers
        });
        
        if (response.status === 401) {
            // Token expired, redirect to login
            this.removeToken();
            window.location.href = '/login.html';
            throw new Error('Unauthorized');
        }
        
        return response;
    }
    
    // ============ Authentication ============
    async login(email, password) {
        const response = await this.request('/auth/token/', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        return response.json();
    }
    
    async getProfile() {
        const response = await this.request('/accounts/profile/');
        return response.json();
    }
    
    async updateProfile(data) {
        const response = await this.request('/accounts/profile/', {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        return response.json();
    }
    
    // ============ Churches ============
    async getChurches(params = {}) {
        const query = new URLSearchParams(params).toString();
        const response = await this.request(`/churches/?${query}`);
        return response.json();
    }
    
    async getChurchDetails(churchId) {
        const response = await this.request(`/churches/${churchId}/`);
        return response.json();
    }
    
    async joinChurch(churchId) {
        const response = await this.request(`/churches/${churchId}/join/`, {
            method: 'POST'
        });
        return response.json();
    }
    
    async transferChurch(fromChurchId, toChurchId, reason) {
        const response = await this.request('/churches/transfer/', {
            method: 'POST',
            body: JSON.stringify({
                from_church_id: fromChurchId,
                to_church_id: toChurchId,
                reason: reason
            })
        });
        return response.json();
    }
    
    async getChurchMembers(churchId) {
        const response = await this.request(`/churches/${churchId}/members/`);
        return response.json();
    }
    
    async getPendingChurches() {
        const response = await this.request('/churches/pending-approval/');
        return response.json();
    }
    
    async approveChurch(churchId) {
        const response = await this.request(`/churches/${churchId}/approve/`, {
            method: 'POST'
        });
        return response.json();
    }
    
    async rejectChurch(churchId, reason) {
        const response = await this.request(`/churches/${churchId}/reject/`, {
            method: 'POST',
            body: JSON.stringify({ reason })
        });
        return response.json();
    }
    
    // ============ Giving ============
    async getGivingHistory(params = {}) {
        const query = new URLSearchParams(params).toString();
        const response = await this.request(`/giving/transactions/history/?${query}`);
        return response.json();
    }
    
    async getGivingSummary() {
        const response = await this.request('/giving/transactions/summary/');
        return response.json();
    }
    
    async createGiving(amount, givingType, churchId, note = null) {
        const response = await this.request('/giving/transactions/', {
            method: 'POST',
            body: JSON.stringify({
                amount,
                giving_type: givingType,
                church_id: churchId,
                note
            })
        });
        return response.json();
    }
    
    async getChurchGivings(churchId) {
        const response = await this.request(`/giving/church/${churchId}/`);
        return response.json();
    }
    
    // ============ Payments ============
    async initializePayment(amount, givingType, churchId) {
        const response = await this.request('/payments/payments/initialize_paystack/', {
            method: 'POST',
            body: JSON.stringify({
                amount,
                giving_type: givingType,
                church_id: churchId
            })
        });
        return response.json();
    }
    
    async verifyPayment(reference) {
        const response = await this.request(`/payments/payments/verify_payment/?reference=${reference}`);
        return response.json();
    }
    
    // ============ Reports ============
    async getFinancialSummary(params = {}) {
        const query = new URLSearchParams(params).toString();
        const response = await this.request(`/reports/financial-summary/?${query}`);
        return response.json();
    }
    
    async getGivingTrends(period = 'monthly') {
        const response = await this.request(`/reports/giving-trends/?period=${period}`);
        return response.json();
    }
    
    async getMemberStatistics(churchId = null) {
        const query = churchId ? `?church_id=${churchId}` : '';
        const response = await this.request(`/reports/member-statistics/${query}`);
        return response.json();
    }
    
    async getChurchPerformance(churchId = null) {
        const query = churchId ? `?church_id=${churchId}` : '';
        const response = await this.request(`/reports/church-performance/${query}`);
        return response.json();
    }
    
    async getSystemOverview() {
        const response = await this.request('/reports/system-overview/');
        return response.json();
    }
}

// Create global instance
const api = new AltarFundsAPI();
```

### Step 2: Create Login Page

Create `web/login.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - AltarFunds</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
        }
        .login-card {
            background: white;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.1);
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-md-5">
                <div class="login-card p-5">
                    <div class="text-center mb-4">
                        <h2 class="fw-bold">AltarFunds</h2>
                        <p class="text-muted">Church Management System</p>
                    </div>
                    
                    <div id="error-message" class="alert alert-danger d-none"></div>
                    
                    <form id="login-form">
                        <div class="mb-3">
                            <label for="email" class="form-label">Email Address</label>
                            <input type="email" class="form-control" id="email" required>
                        </div>
                        
                        <div class="mb-3">
                            <label for="password" class="form-label">Password</label>
                            <input type="password" class="form-control" id="password" required>
                        </div>
                        
                        <button type="submit" class="btn btn-primary w-100 py-2" id="login-btn">
                            <span id="login-text">Login</span>
                            <span id="login-spinner" class="spinner-border spinner-border-sm d-none"></span>
                        </button>
                    </form>
                    
                    <div class="text-center mt-3">
                        <a href="/register.html" class="text-decoration-none">Don't have an account? Register</a>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="assets/js/api.js"></script>
    <script>
        document.getElementById('login-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            
            // Show loading
            document.getElementById('login-text').classList.add('d-none');
            document.getElementById('login-spinner').classList.remove('d-none');
            document.getElementById('login-btn').disabled = true;
            
            try {
                const data = await api.login(email, password);
                
                if (data.access) {
                    // Store tokens
                    api.setToken(data.access);
                    localStorage.setItem('refresh_token', data.refresh);
                    
                    // Redirect to dashboard
                    window.location.href = '/dashboard.html';
                } else {
                    showError('Login failed. Please check your credentials.');
                }
            } catch (error) {
                showError('Network error. Please try again.');
            } finally {
                // Hide loading
                document.getElementById('login-text').classList.remove('d-none');
                document.getElementById('login-spinner').classList.add('d-none');
                document.getElementById('login-btn').disabled = false;
            }
        });
        
        function showError(message) {
            const errorDiv = document.getElementById('error-message');
            errorDiv.textContent = message;
            errorDiv.classList.remove('d-none');
        }
    </script>
</body>
</html>
```

### Step 3: Create Dashboard

Create `web/dashboard.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - AltarFunds</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <!-- Navbar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container-fluid">
            <a class="navbar-brand fw-bold" href="#">AltarFunds</a>
            <div class="d-flex">
                <span class="navbar-text text-white me-3" id="user-name"></span>
                <button class="btn btn-outline-light btn-sm" onclick="logout()">Logout</button>
            </div>
        </div>
    </nav>

    <div class="container-fluid mt-4">
        <div class="row">
            <!-- Sidebar -->
            <div class="col-md-2 bg-light p-3">
                <ul class="nav flex-column">
                    <li class="nav-item">
                        <a class="nav-link active" href="#dashboard">Dashboard</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="#givings">My Givings</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="#church">My Church</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="#profile">Profile</a>
                    </li>
                </ul>
            </div>
            
            <!-- Main Content -->
            <div class="col-md-10">
                <h2 class="mb-4">Financial Dashboard</h2>
                
                <!-- Summary Cards -->
                <div class="row">
                    <div class="col-md-3">
                        <div class="card text-white bg-success mb-3">
                            <div class="card-body">
                                <h6 class="card-title">Total Income</h6>
                                <h3 id="total-income">₦0.00</h3>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card text-white bg-danger mb-3">
                            <div class="card-body">
                                <h6 class="card-title">Total Expenses</h6>
                                <h3 id="total-expenses">₦0.00</h3>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card text-white bg-info mb-3">
                            <div class="card-body">
                                <h6 class="card-title">Net Income</h6>
                                <h3 id="net-income">₦0.00</h3>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="card text-white bg-warning mb-3">
                            <div class="card-body">
                                <h6 class="card-title">Budget Used</h6>
                                <h3 id="budget-utilization">0%</h3>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Charts -->
                <div class="row mt-4">
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-body">
                                <h5 class="card-title">Income by Category</h5>
                                <canvas id="income-chart"></canvas>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-body">
                                <h5 class="card-title">Expenses by Category</h5>
                                <canvas id="expenses-chart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="assets/js/api.js"></script>
    <script>
        async function loadDashboard() {
            try {
                // Load user profile
                const profile = await api.getProfile();
                if (profile.success) {
                    document.getElementById('user-name').textContent = profile.data.first_name;
                }
                
                // Load financial summary
                const summary = await api.getFinancialSummary();
                if (summary.success) {
                    updateDashboard(summary.data);
                }
            } catch (error) {
                console.error('Error loading dashboard:', error);
            }
        }
        
        function updateDashboard(data) {
            // Update cards
            document.getElementById('total-income').textContent = `₦${data.total_income.toLocaleString()}`;
            document.getElementById('total-expenses').textContent = `₦${data.total_expenses.toLocaleString()}`;
            document.getElementById('net-income').textContent = `₦${data.net_income.toLocaleString()}`;
            document.getElementById('budget-utilization').textContent = `${data.budget_utilization}%`;
            
            // Create income chart
            if (data.income_by_category && data.income_by_category.length > 0) {
                const incomeCtx = document.getElementById('income-chart').getContext('2d');
                new Chart(incomeCtx, {
                    type: 'pie',
                    data: {
                        labels: data.income_by_category.map(c => c.category__name),
                        datasets: [{
                            data: data.income_by_category.map(c => c.total),
                            backgroundColor: ['#28a745', '#17a2b8', '#ffc107', '#dc3545', '#6c757d']
                        }]
                    }
                });
            }
            
            // Create expenses chart
            if (data.expenses_by_category && data.expenses_by_category.length > 0) {
                const expensesCtx = document.getElementById('expenses-chart').getContext('2d');
                new Chart(expensesCtx, {
                    type: 'pie',
                    data: {
                        labels: data.expenses_by_category.map(c => c.category__name),
                        datasets: [{
                            data: data.expenses_by_category.map(c => c.total),
                            backgroundColor: ['#dc3545', '#fd7e14', '#6c757d', '#343a40', '#e83e8c']
                        }]
                    }
                });
            }
        }
        
        function logout() {
            api.removeToken();
            window.location.href = '/login.html';
        }
        
        // Load dashboard on page load
        loadDashboard();
    </script>
</body>
</html>
```

---

## ✅ Testing Integration

### Test Backend APIs

```bash
# Start Django server
python manage.py runserver

# Test login
curl -X POST http://127.0.0.1:8000/api/auth/token/ \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Test financial summary
curl -X GET http://127.0.0.1:8000/api/reports/financial-summary/ \
  -H "Authorization: Bearer <token>"
```

### Test Mobile App

1. Update `ApiService.kt` BASE_URL to your backend
2. Build and run the app
3. Test login and payment flow

### Test Web App

1. Open `web/login.html` in browser
2. Login with test credentials
3. View dashboard with real data from backend

---

## 🚀 Deployment

### Backend
```bash
# Collect static files
python manage.py collectstatic

# Run migrations
python manage.py migrate

# Create superuser
python manage.py createsuperuser
```

### Mobile App
- Update BASE_URL to production
- Add production Paystack keys
- Build release APK

### Web App
- Deploy to web server (Nginx, Apache)
- Update API_BASE_URL to production
- Enable HTTPS

---

**Integration Complete!** ✅

Both mobile and web apps are now fully integrated with the Django backend.
