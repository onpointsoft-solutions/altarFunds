package com.altarfunds.mobile.models

data class Budget(
    val id: String,
    val name: String,
    val category: String,
    val amount: Double,
    val spent: Double,
    val remaining: Double,
    val period: String,
    val startDate: String,
    val endDate: String,
    val isActive: Boolean
)
