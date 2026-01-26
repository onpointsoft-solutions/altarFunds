package com.altarfunds.mobile.models

data class EmergencyContact(
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val email: String? = null
)
