package com.altarfunds.member.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.api.ApiService
import com.altarfunds.member.api.RetrofitClient
import com.altarfunds.member.models.*
import com.altarfunds.member.utils.TokenManager
import kotlinx.coroutines.launch

class GivingViewModel : ViewModel() {
    
    private var apiService: ApiService? = null
    private val app by lazy { MemberApp.getInstance() }
    fun initializeApiService(tokenManager: TokenManager) {
        apiService = RetrofitClient.create(tokenManager)
    }
    
    private fun getApiService(): ApiService {
        return apiService ?: throw IllegalStateException("API Service not initialized. Call initializeApiService() first.")
    }
    
    private val _givingCategories = MutableLiveData<Resource<List<GivingCategory>>>()
    val givingCategories: LiveData<Resource<List<GivingCategory>>> = _givingCategories
    
    private val _paymentDetails = MutableLiveData<Resource<ChurchPaymentDetails>>()
    val paymentDetails: LiveData<Resource<ChurchPaymentDetails>> = _paymentDetails
    
    private val _themeColors = MutableLiveData<Resource<ChurchThemeColors>>()
    val themeColors: LiveData<Resource<ChurchThemeColors>> = _themeColors
    
    private val _donationResult = MutableLiveData<Resource<GivingTransaction>>()
    val donationResult: LiveData<Resource<GivingTransaction>> = _donationResult
    
    private val _paystackPaymentResult = MutableLiveData<Resource<PaystackPaymentResponse>>()
    val paystackPaymentResult: LiveData<Resource<PaystackPaymentResponse>> = _paystackPaymentResult
    
        
    fun loadGivingCategories() {
        _givingCategories.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = getApiService().getGivingCategories()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _givingCategories.value = Resource.Success(apiResponse?.data ?: emptyList())
                    } else {
                        _givingCategories.value = Resource.Error(apiResponse?.message ?: "Failed to load categories")
                    }
                } else {
                    _givingCategories.value = Resource.Error("Failed to load categories: ${response.code()}")
                }
            } catch (e: Exception) {
                _givingCategories.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun loadPaymentDetails() {
        _paymentDetails.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = getApiService().getChurchPaymentDetails()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _paymentDetails.value = Resource.Success(apiResponse?.data) as Resource<ChurchPaymentDetails>?
                    } else {
                        _paymentDetails.value = Resource.Error(apiResponse?.message ?: "Failed to load payment details")
                    }
                } else {
                    _paymentDetails.value = Resource.Error("Failed to load payment details: ${response.code()}")
                }
            } catch (e: Exception) {
                _paymentDetails.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun loadThemeColors() {
        _themeColors.value = Resource.Loading
        viewModelScope.launch {
            try {
                val response = getApiService().getChurchThemeColors()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _themeColors.value = Resource.Success(apiResponse?.data) as Resource<ChurchThemeColors>?
                    } else {
                        _themeColors.value = Resource.Error(apiResponse?.message ?: "Failed to load theme colors")
                    }
                } else {
                    _themeColors.value = Resource.Error("Failed to load theme colors: ${response.code()}")
                }
            } catch (e: Exception) {
                _themeColors.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }
    
        
    fun createDonation(
        categoryId: Int,
        amount: Double,
        paymentMethod: String,
        reference: String? = null
    ) {
        _donationResult.value = Resource.Loading
        viewModelScope.launch {
            try {
                val givingRequest = GivingTransactionRequest(
                    category = categoryId,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    note = reference ?: "Mobile donation",
                    isAnonymous = false
                )
                
                val response = getApiService().createDonation(givingRequest)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true && apiResponse.data != null) {
                        _donationResult.value = Resource.Success(apiResponse.data)
                    } else {
                        _donationResult.value = Resource.Error(apiResponse?.message ?: "Failed to create donation")
                    }
                } else {
                    _donationResult.value = Resource.Error("Failed to create donation: ${response.code()}")
                }
            } catch (e: Exception) {
                _donationResult.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun initializePaystackPayment(
        amount: String,
        givingType: String,
        churchId: Int,
        email: String
    ) {
        _paystackPaymentResult.value = Resource.Loading
        viewModelScope.launch {
            try {
                val paystackRequest = PaystackPaymentRequest(
                    email = email,
                    amount = amount,
                    givingType = givingType,
                    churchId = churchId,
                    metadata = mapOf(
                        "user_id" to app.tokenManager.getRefreshToken(),
                        "user_email" to email
                    ) as Map<String, Any>
                )
                
                val response = getApiService().initializePaystackPayment(paystackRequest)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true && apiResponse?.data != null) {
                        val paymentData = apiResponse.data
                        val paystackResponse = PaystackPaymentResponse(
                            authorizationUrl = paymentData["authorization_url"] as String,
                            reference = paymentData["reference"] as String,
                            accessCode = paymentData["access_code"] as String
                        )
                        _paystackPaymentResult.value = Resource.Success(paystackResponse)
                    } else {
                        _paystackPaymentResult.value = Resource.Error(apiResponse?.message ?: "Failed to initialize payment")
                    }
                } else {
                    _paystackPaymentResult.value = Resource.Error("Failed to initialize payment: ${response.code()}")
                }
            } catch (e: Exception) {
                _paystackPaymentResult.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun verifyPaystackPayment(reference: String) {
        viewModelScope.launch {
            try {
                val response = getApiService().verifyPaystackPayment(reference)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        // Payment verified successfully
                        // You might want to refresh giving transactions here
                    }
                }
            } catch (e: Exception) {
                // Handle verification error
            }
        }
    }
}

// Resource wrapper for API responses
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
