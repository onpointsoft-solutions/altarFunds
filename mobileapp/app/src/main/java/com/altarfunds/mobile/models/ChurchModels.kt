package com.altarfunds.mobile.models

data class Church(
    val id: String,
    val name: String,
    val code: String,
    val address: String,
    val city: String,
    val state: String,
    val country: String,
    val phoneNumber: String,
    val email: String,
    val website: String,
    val description: String,
    val imageUrl: String?,
    val isVerified: Boolean,
    val isActive: Boolean,
    val memberCount: Int,
    val foundedDate: String,
    val denomination: String,
    val serviceTimes: List<ServiceTime>,
    val leadership: List<Leadership>,
    val ministries: List<Ministry>
)

data class ServiceTime(
    val day: String,
    val time: String,
    val type: String // "worship", "bible_study", "prayer", etc.
)

data class Leadership(
    val id: String,
    val name: String,
    val title: String,
    val email: String,
    val phoneNumber: String,
    val imageUrl: String?,
    val bio: String
)

data class Ministry(
    val id: String,
    val name: String,
    val description: String,
    val leader: String,
    val meetingTime: String,
    val isActive: Boolean
)

data class ChurchJoinResponse(
    val success: Boolean,
    val message: String,
    val applicationId: String,
    val status: String, // "pending", "approved", "rejected"
    val nextSteps: List<String>
)

data class ChurchSearchRequest(
    val query: String,
    val location: String?,
    val radius: Int?, // in kilometers
    val denomination: String?,
    val serviceDay: String?
)

data class ChurchTransferRequest(
    val currentChurchId: Any,
    val newChurchId: String,
    val reason: String,
    val transferDate: String,
    val notifyCurrentChurch: Boolean,
    val requestMembershipLetter: Boolean
)


data class ChurchMembershipStatus(
    val churchId: String,
    val churchName: String,
    val membershipType: String,
    val joinDate: String,
    val status: String, // "active", "inactive", "pending"
    val roles: List<String>, // "member", "leader", "volunteer", etc.
    val contributions: List<Contribution>
)

data class Contribution(
    val id: String,
    val type: String, // "tithe", "offering", "special"
    val amount: Double,
    val date: String,
    val frequency: String // "weekly", "monthly", "one-time"
)
