package com.sanctum.member.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanctum.member.MemberApp
import com.sanctum.member.models.*
import kotlinx.coroutines.launch

class GivingViewModel : ViewModel() {

    // Always use the app-level ApiService — never requires manual initialization.
    private val app by lazy { MemberApp.getInstance() }
    private val api get() = app.apiService

    // ── Giving Categories ─────────────────────────────────────────────────
    private val _givingCategories = MutableLiveData<Resource<List<GivingCategory>>>()
    val givingCategories: LiveData<Resource<List<GivingCategory>>> = _givingCategories

    fun loadGivingCategories() {
        _givingCategories.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = api.getGivingCategories()
                if (response.isSuccessful && response.body()?.success == true) {
                    _givingCategories.value = Resource.Success(response.body()!!.data ?: emptyList())
                } else {
                    _givingCategories.value = Resource.Error(
                        response.body()?.message ?: "Failed to load categories (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _givingCategories.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }

    // ── Giving Transactions ───────────────────────────────────────────────
    private val _givingTransactions = MutableLiveData<Resource<List<GivingTransaction>>>()
    val givingTransactions: LiveData<Resource<List<GivingTransaction>>> = _givingTransactions

    fun loadGivingTransactions() {
        _givingTransactions.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = api.getGivingTransactions()
                if (response.isSuccessful && response.body() != null) {
                    _givingTransactions.value = Resource.Success(response.body()!!.results)
                } else {
                    _givingTransactions.value = Resource.Error(
                        "Failed to load transactions (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _givingTransactions.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }

    // ── Donation / Payment ────────────────────────────────────────────────
    private val _donationResult = MutableLiveData<Resource<GivingTransaction>>()
    val donationResult: LiveData<Resource<GivingTransaction>> = _donationResult

    fun initiatePayment(
        amount: Double,
        categoryId: Int,
        paymentMethod: String,
        phoneNumber: String? = null,
        email: String? = null
    ) {
        _donationResult.value = Resource.Loading
        viewModelScope.launch {
            try {
                val request = GivingTransactionRequest(
                    category      = categoryId,
                    amount        = amount,
                    paymentMethod = paymentMethod,
                    note          = "Mobile donation via $paymentMethod",
                    isAnonymous   = false,
                    phoneNumber   = phoneNumber,
                    email         = email,
                )
                val response = api.createDonation(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val tx = response.body()!!.data
                    if (tx != null) {
                        _donationResult.value = Resource.Success(tx)
                    } else {
                        _donationResult.value = Resource.Error("No transaction data returned")
                    }
                } else {
                    _donationResult.value = Resource.Error(
                        response.body()?.message ?: "Payment failed (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _donationResult.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }

    // ── Paystack card payment ─────────────────────────────────────────────
    private val _paystackAuthUrl = MutableLiveData<Resource<String>>()
    val paystackAuthUrl: LiveData<Resource<String>> = _paystackAuthUrl

    fun initiateCardPayment(amount: Double, categoryId: Int, email: String?) {
        _paystackAuthUrl.value = Resource.Loading
        viewModelScope.launch {
            try {
                val request = GivingTransactionRequest(
                    category      = categoryId,
                    amount        = amount,
                    paymentMethod = "card",
                    note          = "Card payment via Paystack",
                    isAnonymous   = false,
                    email         = email,
                )
                val response = api.createDonationWithPaystack(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val url  = body.authorizationUrl
                    if (!url.isNullOrBlank()) {
                        _paystackAuthUrl.value = Resource.Success(url)
                        body.data?.let { _donationResult.value = Resource.Success(it) }
                    } else {
                        _paystackAuthUrl.value = Resource.Error(
                            body.warning ?: body.message ?: "Paystack did not return an authorization URL"
                        )
                    }
                } else {
                    _paystackAuthUrl.value = Resource.Error("Payment init failed (${response.code()})")
                }
            } catch (e: Exception) {
                _paystackAuthUrl.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }

    fun checkPaymentStatus(donationId: Int) {
        viewModelScope.launch {
            try {
                val response = api.getGivingTransactions()
                if (response.isSuccessful) {
                    val tx = response.body()?.results?.find { it.id == donationId }
                    tx?.let {
                        when (it.paymentStatus?.lowercase()) {
                            "completed" -> _donationResult.value = Resource.Success(it)
                            "failed"    -> _donationResult.value = Resource.Error("Payment failed")
                        }
                    }
                }
            } catch (_: Exception) { /* continue polling */ }
        }
    }

    // ── Budget PIN verification ───────────────────────────────────────────
    private val _budgetSummary = MutableLiveData<Resource<BudgetSummaryResponse>>()
    val budgetSummary: LiveData<Resource<BudgetSummaryResponse>> = _budgetSummary

    fun verifyBudgetPin(pin: String) {
        _budgetSummary.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = api.verifyBudgetPin(BudgetPinRequest(pin))
                if (response.isSuccessful && response.body()?.success == true) {
                    _budgetSummary.value = Resource.Success(response.body()!!)
                } else {
                    val msg = response.body()?.error
                        ?: "Invalid or expired PIN (${response.code()})"
                    _budgetSummary.value = Resource.Error(msg)
                }
            } catch (e: Exception) {
                _budgetSummary.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }

    // ── Church details ────────────────────────────────────────────────────
    private val _paymentDetails = MutableLiveData<Resource<ChurchPaymentDetails>>()
    val paymentDetails: LiveData<Resource<ChurchPaymentDetails>> = _paymentDetails

    private val _themeColors = MutableLiveData<Resource<ChurchThemeColors>>()
    val themeColors: LiveData<Resource<ChurchThemeColors>> = _themeColors

    fun loadPaymentDetails() {
        _paymentDetails.value = Resource.Loading
        viewModelScope.launch {
            try {
                val r = api.getChurchPaymentDetails()
                if (r.isSuccessful && r.body()?.success == true) {
                    _paymentDetails.value = Resource.Success(r.body()!!.data!!)
                } else {
                    _paymentDetails.value = Resource.Error("Failed (${r.code()})")
                }
            } catch (e: Exception) {
                _paymentDetails.value = Resource.Error(e.message ?: "Error")
            }
        }
    }

    fun loadThemeColors() {
        _themeColors.value = Resource.Loading
        viewModelScope.launch {
            try {
                val r = api.getChurchThemeColors()
                if (r.isSuccessful && r.body()?.success == true) {
                    _themeColors.value = Resource.Success(r.body()!!.data!!)
                } else {
                    _themeColors.value = Resource.Error("Failed (${r.code()})")
                }
            } catch (e: Exception) {
                _themeColors.value = Resource.Error(e.message ?: "Error")
            }
        }
    }
}

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
