package com.altarfunds.mobile.models

data class Transaction(
    val id: String,
    val type: String, // "income", "expense", "donation"
    val amount: Double,
    val description: String,
    val category: String,
    val date: String,
    val status: String, // "completed", "pending", "failed"
    val reference: String? = null,
    val metadata: Map<String, Any>? = null
)
