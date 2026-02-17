package com.altarfunds.member.api

import com.altarfunds.member.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentication
    @POST("auth/token/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("accounts/register/")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
    
    @POST("auth/token/refresh/")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>
    
    @POST("accounts/forgot-password/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>
    
    // User Profile
    @GET("accounts/profile/")
    suspend fun getProfile(): Response<User>
    
    @PUT("accounts/profile/")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>
    
    @POST("accounts/change-password/")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>
    
    // Churches
    @GET("churches/")
    suspend fun getChurches(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1
    ): Response<PaginatedResponse<Church>>
    
    @GET("churches/{id}/")
    suspend fun getChurchDetails(@Path("id") churchId: Int): Response<Church>
    
    @POST("churches/{id}/join/")
    suspend fun joinChurch(@Path("id") churchId: Int): Response<MessageResponse>
    
    // Giving Categories
    @GET("giving/categories/")
    suspend fun getGivingCategories(): Response<ApiResponse<List<GivingCategory>>>
    
    // Payment Initialization
    @POST("giving/initialize-payment/")
    suspend fun initializePayment(@Body request: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>
    
    // Payment Verification
    @POST("giving/verify-payment/")
    suspend fun verifyPayment(@Body request: Map<String, String>): Response<ApiResponse<Map<String, Any>>>
    
    // Church Payment Details
    @GET("churches/mobile/payment-details/")
    suspend fun getChurchPaymentDetails(): Response<ApiResponse<ChurchPaymentDetails>>
    
    // Church Theme Colors
    @GET("churches/mobile/theme-colors/")
    suspend fun getChurchThemeColors(): Response<ApiResponse<ChurchThemeColors>>
    
    // Donations/Giving
    @POST("mobile/donations/")
    suspend fun createDonation(@Body request: DonationRequest): Response<Donation>
    
    @GET("mobile/donations/")
    suspend fun getDonations(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedResponse<Donation>>
    
    @GET("mobile/donations/{id}/")
    suspend fun getDonationDetails(@Path("id") donationId: Int): Response<Donation>
    
    // M-Pesa Payment
    @POST("mobile/mpesa/stk-push/")
    suspend fun initiateMpesaPayment(@Body request: MpesaRequest): Response<MpesaResponse>
    
    @GET("mobile/mpesa/status/{transaction_id}/")
    suspend fun checkPaymentStatus(@Path("transaction_id") transactionId: String): Response<PaymentStatus>
    
    // Announcements
    @GET("announcements/")
    suspend fun getAnnouncements(
        @Query("page") page: Int = 1
    ): Response<PaginatedResponse<Announcement>>
    
    @GET("announcements/{id}/")
    suspend fun getAnnouncementDetails(@Path("id") announcementId: Int): Response<Announcement>
    
    // Devotionals
    @GET("devotionals/")
    suspend fun getDevotionals(
        @Query("page") page: Int = 1
    ): Response<PaginatedResponse<Devotional>>
    
    @GET("devotionals/{id}/")
    suspend fun getDevotionalDetails(@Path("id") devotionalId: Int): Response<Devotional>
    
    // Dashboard Stats
    @GET("dashboard/stats/")
    suspend fun getDashboardStats(): Response<DashboardStats>
    
    // Suggestions
    @GET("suggestions/")
    suspend fun getSuggestions(): Response<List<Suggestion>>
    
    @POST("suggestions/")
    suspend fun createSuggestion(@Body request: SuggestionRequest): Response<Suggestion>
    
    // Church Join Requests
    @GET("auth/check-approval-status/")
    suspend fun checkApprovalStatus(): Response<ChurchJoinRequest>
    
    // Church Transfer
    @POST("auth/transfer-church/")
    suspend fun transferChurch(@Body request: ChurchTransferRequest): Response<MessageResponse>
}
