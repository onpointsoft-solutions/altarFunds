package com.altarfunds.mobile.models

data class Donation(
    val id: String,
    val amount: Double,
    val donorName: String,
    val donorEmail: String,
    val donorPhone: String,
    val churchId: String,
    val churchName: String,
    val fundType: String,
    val paymentMethod: String,
    val status: String,
    val date: String,
    val reference: String? = null,
    val notes: String? = null
)
