package com.altarfunds.mobile.models

// API Response wrapper classes
data class DonationListResponse(
    val donations: List<Donation>,
    val count: Int,
    val total: Double
)

data class ExpenseListResponse(
    val expenses: List<Expense>,
    val count: Int,
    val total: Double
)

data class BudgetListResponse(
    val budgets: List<Budget>,
    val count: Int,
    val total: Double
)

data class MemberListResponse(
    val members: List<Member>,
    val count: Int
)

data class ChurchSearchResult(
    val id: String,
    val name: String,
    val church_code: String,
    val location: String,
    val city: String? = null,
    val memberCount: Int,
    val distance: Double? = null,
    val is_verified: Boolean = false,
    val church_type: String? = null
)

data class RejectReasonRequest(
    val reason: String,
    val notes: String? = null
)
