package com.altarfunds.member.models

import com.google.gson.annotations.SerializedName

// Generic Response Models
data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

data class MessageResponse(
    val message: String
)

// Authentication Models
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String,
    val user: User
)

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("password_confirm") val passwordConfirm: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("phone_number") val phoneNumber: String
)

data class RegisterResponse(
    val user: User,
    val message: String
)

data class RefreshTokenRequest(
    val refresh: String
)

data class TokenResponse(
    val access: String
)

data class ForgotPasswordRequest(
    val email: String
)

// User Models
data class User(
    val id: Int,
    val email: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("phone_number") val phoneNumber: String?,
    val role: String,
    val church: Int?,
    @SerializedName("church_info") val churchInfo: ChurchInfo?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("date_joined") val dateJoined: String
)

data class ChurchInfo(
    val id: Int,
    val name: String,
    val code: String
)

data class UpdateProfileRequest(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("phone_number") val phoneNumber: String
)

data class ChangePasswordRequest(
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

// Church Models
data class Church(
    val id: Int,
    val name: String,
    val code: String,
    val email: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("address_line1") val addressLine1: String?,
    val city: String?,
    val country: String?,
    val website: String?,
    val description: String?,
    val denomination: Int?,
    @SerializedName("denomination_name") val denominationName: String?,
    @SerializedName("member_count") val memberCount: Int?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String
)

// Donation Models
data class DonationRequest(
    val amount: String,
    @SerializedName("donation_type") val donationType: String,
    val description: String?,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("phone_number") val phoneNumber: String?
)

data class Donation(
    val id: Int,
    val amount: String,
    @SerializedName("donation_type") val donationType: String,
    @SerializedName("donation_type_display") val donationTypeDisplay: String,
    val description: String?,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_method_display") val paymentMethodDisplay: String,
    val status: String,
    @SerializedName("status_display") val statusDisplay: String,
    @SerializedName("transaction_id") val transactionId: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("donor_name") val donorName: String,
    @SerializedName("church_name") val churchName: String,
    @SerializedName("created_at") val createdAt: String
)

// M-Pesa Models
data class MpesaRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    val amount: String,
    @SerializedName("donation_type") val donationType: String,
    val description: String?
)

data class MpesaResponse(
    @SerializedName("merchant_request_id") val merchantRequestId: String,
    @SerializedName("checkout_request_id") val checkoutRequestId: String,
    @SerializedName("response_code") val responseCode: String,
    @SerializedName("response_description") val responseDescription: String,
    @SerializedName("customer_message") val customerMessage: String
)

data class PaymentStatus(
    val status: String,
    @SerializedName("transaction_id") val transactionId: String?,
    val message: String
)

// Announcement Models
data class Announcement(
    val id: Int,
    val title: String,
    val content: String,
    val priority: String,
    @SerializedName("priority_display") val priorityDisplay: String,
    @SerializedName("target_audience") val targetAudience: String,
    @SerializedName("target_audience_display") val targetAudienceDisplay: String,
    @SerializedName("church_name") val churchName: String,
    @SerializedName("created_by_name") val createdByName: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("is_expired") val isExpired: Boolean,
    @SerializedName("created_at") val createdAt: String
)

// Devotional Models
data class Devotional(
    val id: Int,
    val title: String,
    val content: String,
    @SerializedName("scripture_reference") val scriptureReference: String?,
    val author: String,
    val date: String,
    @SerializedName("created_at") val createdAt: String
)

// Dashboard Models
data class DashboardStats(
    @SerializedName("total_donations") val totalDonations: String,
    @SerializedName("donation_count") val donationCount: Int,
    @SerializedName("recent_donations") val recentDonations: List<Donation>,
    @SerializedName("announcements_count") val announcementsCount: Int,
    @SerializedName("devotionals_count") val devotionalsCount: Int
)

// Suggestion Models
data class Suggestion(
    val id: Int,
    @SerializedName("member_name") val memberName: String,
    @SerializedName("member_email") val memberEmail: String?,
    val title: String,
    val description: String,
    val category: String,
    @SerializedName("category_display") val categoryDisplay: String,
    val status: String,
    @SerializedName("status_display") val statusDisplay: String,
    @SerializedName("is_anonymous") val isAnonymous: Boolean,
    val response: String?,
    @SerializedName("reviewed_by_name") val reviewedByName: String?,
    @SerializedName("reviewed_at") val reviewedAt: String?,
    @SerializedName("created_at") val createdAt: String
)

data class SuggestionRequest(
    val title: String,
    val description: String,
    val category: String,
    @SerializedName("is_anonymous") val isAnonymous: Boolean
)

// Church Join Request Models
data class ChurchJoinRequest(
    val id: Int,
    val user: User,
    @SerializedName("church_code") val churchCode: String,
    val status: String,
    val message: String?,
    @SerializedName("reviewed_by") val reviewedBy: User?,
    @SerializedName("reviewed_at") val reviewedAt: String?,
    @SerializedName("rejection_reason") val rejectionReason: String?,
    @SerializedName("created_at") val createdAt: String
)
