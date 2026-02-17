package com.altarfunds.member.data.mappers

import com.altarfunds.member.data.local.entities.*
import com.altarfunds.member.models.*

// User mappers
fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        phoneNumber = phoneNumber,
        role = role,
        church = church,
        churchName = churchInfo?.name,
        churchCode = churchInfo?.code,
        isActive = isActive,
        dateJoined = dateJoined
    )
}

fun UserEntity.toModel(): User {
    return User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        phoneNumber = phoneNumber,
        role = role,
        church = church,
        churchInfo = if (churchName != null && churchCode != null) {
            ChurchInfo(id = church ?: 0, name = churchName, code = churchCode)
        } else null,
        isActive = isActive,
        dateJoined = dateJoined
    )
}

// Church mappers
fun Church.toEntity(): ChurchEntity {
    return ChurchEntity(
        id = id,
        name = name,
        code = code,
        email = email,
        phoneNumber = phoneNumber,
        addressLine1 = addressLine1,
        city = city,
        country = country,
        website = website,
        description = description,
        denomination = denomination,
        denominationName = denominationName,
        memberCount = memberCount,
        isActive = isActive,
        createdAt = createdAt
    )
}

fun ChurchEntity.toModel(): Church {
    return Church(
        id = id,
        name = name,
        code = code,
        email = email,
        phoneNumber = phoneNumber,
        addressLine1 = addressLine1,
        city = city,
        country = country,
        website = website,
        description = description,
        denomination = denomination,
        denominationName = denominationName,
        memberCount = memberCount,
        isActive = isActive,
        createdAt = createdAt
    )
}

// Donation mappers
fun Donation.toEntity(): DonationEntity {
    return DonationEntity(
        id = id,
        amount = amount,
        donationType = donationType,
        donationTypeDisplay = donationTypeDisplay,
        description = description,
        paymentMethod = paymentMethod,
        paymentMethodDisplay = paymentMethodDisplay,
        status = status,
        statusDisplay = statusDisplay,
        transactionId = transactionId,
        phoneNumber = phoneNumber,
        donorName = donorName,
        churchName = churchName,
        createdAt = createdAt
    )
}

fun DonationEntity.toModel(): Donation {
    return Donation(
        id = id,
        amount = amount,
        donationType = donationType,
        donationTypeDisplay = donationTypeDisplay,
        description = description,
        paymentMethod = paymentMethod,
        paymentMethodDisplay = paymentMethodDisplay,
        status = status,
        statusDisplay = statusDisplay,
        transactionId = transactionId,
        phoneNumber = phoneNumber,
        donorName = donorName,
        churchName = churchName,
        createdAt = createdAt
    )
}

// Announcement mappers
fun Announcement.toEntity(): AnnouncementEntity {
    return AnnouncementEntity(
        id = id,
        title = title,
        content = content,
        priority = priority,
        priorityDisplay = priorityDisplay,
        authorName = createdByName,
        churchName = churchName,
        isActive = isActive,
        createdAt = createdAt
    )
}

fun AnnouncementEntity.toModel(): Announcement {
    return Announcement(
        id = id,
        title = title,
        content = content,
        priority = priority,
        priorityDisplay = priorityDisplay,
        targetAudience = "all", // Default value since entity doesn't have this field
        targetAudienceDisplay = "All", // Default value
        churchName = churchName,
        createdByName = authorName ?: "Unknown",
        isActive = isActive,
        expiresAt = null, // Default value since entity doesn't have this field
        isExpired = false, // Default value
        createdAt = createdAt
    )
}

// Devotional mappers
fun Devotional.toEntity(): DevotionalEntity {
    return DevotionalEntity(
        id = id,
        title = title,
        content = content,
        scriptureReference = scriptureReference ?: "",
        author = author,
        date = date,
        isActive = true, // Default value since model doesn't have this field
        createdAt = createdAt
    )
}

fun DevotionalEntity.toModel(): Devotional {
    return Devotional(
        id = id,
        title = title,
        content = content,
        scriptureReference = scriptureReference.ifEmpty { null },
        author = author,
        date = date,
        createdAt = createdAt
    )
}

// Suggestion mappers
fun Suggestion.toEntity(): SuggestionEntity {
    return SuggestionEntity(
        id = id,
        memberName = memberName,
        memberEmail = memberEmail,
        title = title,
        description = description,
        category = category,
        categoryDisplay = categoryDisplay,
        status = status,
        statusDisplay = statusDisplay,
        createdAt = createdAt
    )
}

fun SuggestionEntity.toModel(): Suggestion {
    return Suggestion(
        id = id,
        memberName = memberName,
        memberEmail = memberEmail,
        title = title,
        description = description,
        category = category,
        categoryDisplay = categoryDisplay,
        status = status,
        statusDisplay = statusDisplay,
        createdAt = createdAt,
        isAnonymous = TODO(),
        response = TODO(),
        reviewedByName = TODO(),
        reviewedAt = TODO()
    )
}

// DashboardStats mappers
fun DashboardStats.toEntity(): DashboardStatsEntity {
    return DashboardStatsEntity(
        totalDonations = totalDonations,
        donationCount = donationCount,
        announcementsCount = announcementsCount,
        devotionalsCount = devotionalsCount
    )
}

fun DashboardStatsEntity.toModel(): DashboardStats {
    return DashboardStats(
        totalDonations = totalDonations,
        donationCount = donationCount,
        recentDonations = emptyList(), // Will be loaded separately
        announcementsCount = announcementsCount,
        devotionalsCount = devotionalsCount
    )
}
