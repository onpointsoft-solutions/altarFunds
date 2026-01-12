package com.altarfunds.mobile.api

import com.altarfunds.mobile.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiInterface {
    
    // Authentication
    @POST("api/mobile/login/")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("api/mobile/google-login/")
    suspend fun googleLogin(@Body googleLoginRequest: GoogleLoginRequest): Response<LoginResponse>

    @POST("api/mobile/register/")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<LoginResponse>
    
    @POST("api/mobile/register-device/")
    suspend fun registerDevice(@Body deviceRequest: DeviceRegistrationRequest): Response<DeviceRegistrationResponse>
    
    // App Configuration
    @GET("api/mobile/config/")
    suspend fun getAppConfig(@Query("platform") platform: String = "android"): Response<AppConfigResponse>
    
    // User Profile
    @GET("api/mobile/profile/")
    suspend fun getUserProfile(): Response<UserProfileResponse>
    
    // Giving
    @GET("api/mobile/giving-summary/")
    suspend fun getGivingSummary(): Response<GivingSummaryResponse>
    
    @POST("api/giving/transactions/")
    suspend fun createGivingTransaction(@Body transactionRequest: GivingTransactionRequest): Response<GivingTransactionResponse>
    
    @GET("api/giving/transactions/")
    suspend fun getGivingTransactions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<TransactionListResponse>
    
    @POST("api/payments/requests/")
    suspend fun initiatePayment(@Body paymentRequest: PaymentRequest): Response<PaymentResponse>
    
    @POST("api/payments/requests/{request_id}/status/")
    suspend fun checkPaymentStatus(@Path("request_id") requestId: String): Response<PaymentStatusResponse>
    
    // Church Information
    @GET("api/mobile/church-info/")
    suspend fun getChurchInfo(): Response<ChurchInfoResponse>
    
    @GET("api/mobile/quick-actions/")
    suspend fun getQuickActions(): Response<QuickActionsResponse>
    
    // Notifications
    @GET("api/mobile/notifications/")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<NotificationListResponse>
    
    @POST("api/mobile/notifications/{id}/")
    suspend fun markNotificationRead(@Path("id") notificationId: Int): Response<SuccessResponse>
    
    // Analytics
    @POST("api/mobile/analytics/track/")
    suspend fun trackAnalytics(@Body analyticsRequest: AnalyticsRequest): Response<SuccessResponse>
    
    // Feedback
    @POST("api/mobile/feedback/submit/")
    suspend fun submitFeedback(@Body feedbackRequest: FeedbackRequest): Response<FeedbackResponse>
}
