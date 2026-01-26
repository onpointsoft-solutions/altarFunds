package com.altarfunds.mobile.models

data class Expense(
    val id: String,
    val amount: Double,
    val description: String,
    val category: String,
    val churchId: String,
    val churchName: String,
    val requestedBy: String,
    val approvedBy: String? = null,
    val status: String, // "pending", "approved", "rejected"
    val date: String,
    val receiptUrl: String? = null,
    val notes: String? = null,
    val rejectReason: String? = null
)
