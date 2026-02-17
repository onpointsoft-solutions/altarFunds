package com.altarfunds.member.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altarfunds.member.api.ApiService
import com.altarfunds.member.api.RetrofitClient
import com.altarfunds.member.models.*
import com.altarfunds.member.utils.TokenManager
import kotlinx.coroutines.launch

class GivingViewModel : ViewModel() {
    
    private var apiService: ApiService? = null
    
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
    
    private val _donationResult = MutableLiveData<Resource<Donation>>()
    val donationResult: LiveData<Resource<Donation>> = _donationResult
    
        
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
                val donationRequest = DonationRequest(
                    amount = amount,
                    donationType = "one_time",
                    description = "Mobile donation",
                    paymentMethod = paymentMethod,
                    phoneNumber = ""
                )
                
                val response = getApiService().createDonation(donationRequest)
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
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
