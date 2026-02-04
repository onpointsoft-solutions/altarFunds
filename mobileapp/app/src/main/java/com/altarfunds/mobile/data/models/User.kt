package com.altarfunds.mobile.data.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("first_name")
    val firstName: String,
    
    @SerializedName("last_name")
    val lastName: String,
    
    @SerializedName("phone_number")
    val phoneNumber: String?,
    
    @SerializedName("role")
    val role: String,
    
    @SerializedName("church_name")
    val churchName: String?,
    
    @SerializedName("is_email_verified")
    val isEmailVerified: Boolean = false,
    
    @SerializedName("is_phone_verified")
    val isPhoneVerified: Boolean = false,
    
    @SerializedName("profile_picture")
    val profilePicture: String?
)
