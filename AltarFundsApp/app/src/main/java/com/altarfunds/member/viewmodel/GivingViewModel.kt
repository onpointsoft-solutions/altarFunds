package com.altarfunds.member.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altarfunds.member.api.ApiService
import com.altarfunds.member.models.*
import com.altarfunds.member.utils.Resource
import kotlinx.coroutines.launch

class GivingViewModel(private val apiService: ApiService) : ViewModel() {
    
    private val _givingCategories = MutableLiveData<Resource<List<GivingCategory>>>()
    val givingCategories: LiveData<Resource<List<GivingCategory>>> = _givingCategories
    
    private val _paymentDetails = MutableLiveData<Resource<ChurchPaymentDetails>>()
    val paymentDetails: LiveData<Resource<ChurchPaymentDetails>> = _paymentDetails
    
    private val _themeColors = MutableLiveData<Resource<ChurchThemeColors>>()
    val themeColors: LiveData<Resource<ChurchThemeColors>> = _themeColors
    
    private val _donationResult = MutableLiveData<Resource<Donation>>()
    val donationResult: LiveData<Resource<Donation>> = _donationResult
    
    private val _paymentResult = MutableLiveData<Resource<Map<String, Any>>>()
    val paymentResult: LiveData<Resource<Map<String, Any>>> = _paymentResult
    
    fun loadGivingCategories() {
        _givingCategories.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val response = apiService.getGivingCategories()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _givingCategories.value = Resource.Success(apiResponse.data ?: emptyList())
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
        _paymentDetails.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val response = apiService.getChurchPaymentDetails()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _paymentDetails.value = Resource.Success(apiResponse.data!!)
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
        _themeColors.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val response = apiService.getChurchThemeColors()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _themeColors.value = Resource.Success(apiResponse.data!!)
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
    
    fun initializePayment(categoryId: Int, amount: Double) {
        _paymentResult.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val paymentRequest = mapOf(
                    "category_id" to categoryId,
                    "amount" to amount
                )
                
                val response = apiService.initializePayment(paymentRequest)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        _paymentResult.value = Resource.Success(apiResponse.data!!)
                    } else {
                        _paymentResult.value = Resource.Error(apiResponse?.message ?: "Failed to initialize payment")
                    }
                } else {
                    _paymentResult.value = Resource.Error("Failed to initialize payment: ${response.code()}")
                }
            } catch (e: Exception) {
                _paymentResult.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun createDonation(
        categoryId: Int,
        amount: Double,
        paymentMethod: String,
        reference: String? = null
    ) {
        _donationResult.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val donationRequest = DonationRequest(
                    category_id = categoryId,
                    amount = amount,
                    payment_method = paymentMethod,
                    payment_reference = reference,
                    notes = "Mobile donation"
                )
                
                val response = apiService.createDonation(donationRequest)
                if (response.isSuccessful) {
                    _donationResult.value = Resource.Success(response.body()!!)
                } else {
                    _donationResult.value = Resource.Error("Failed to create donation: ${response.code()}")
                }
            } catch (e: Exception) {
                _donationResult.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }
}

// Resource wrapper for API responses
sealed class Resource<T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
    class Loading<T> : Resource<T>()
}
