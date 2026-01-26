package com.altarfunds.mobile.services

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.models.PaystackInitRequest
import kotlinx.coroutines.*

class PaystackPaymentService(private val activity: Activity) {
    
    private val TAG = "PaystackPaymentService"
    private var verificationJob: Job? = null
    
    /**
     * Initiates a Paystack payment
     * @param amount The amount to pay
     * @param givingType The type of giving (tithe, offering, etc.)
     * @param churchId The church ID
     * @param onSuccess Callback when payment is successful with reference
     * @param onError Callback when payment fails with error message
     */
    fun initiatePayment(
        amount: Double,
        givingType: String,
        churchId: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Initializing payment: $$amount for $givingType")
                
                // Call backend API to initialize payment
                val response = ApiService.getApiInterface().initializePaystack(
                    PaystackInitRequest(
                        amount = amount,
                        giving_type = givingType,
                        church_id = churchId
                    )
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val authUrl = data?.authorization_url
                    val reference = data?.reference
                    
                    if (authUrl != null && reference != null) {
                        Log.d(TAG, "Payment initialized successfully: $reference")
                        
                        // Open Paystack checkout in browser
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                        activity.startActivity(intent)
                        
                        // Start verification polling
                        startVerificationPolling(reference, onSuccess, onError)
                    } else {
                        Log.e(TAG, "Invalid payment response")
                        onError("Invalid payment response from server")
                    }
                } else {
                    val errorMsg = response.body()?.message ?: "Payment initialization failed"
                    Log.e(TAG, "Payment init failed: $errorMsg")
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Payment initialization error", e)
                onError(e.message ?: "Network error occurred")
            }
        }
    }
    
    /**
     * Starts polling the backend to verify payment status
     * Polls every 10 seconds for up to 5 minutes
     */
    private fun startVerificationPolling(
        reference: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        verificationJob = CoroutineScope(Dispatchers.Main).launch {
            var attempts = 0
            val maxAttempts = 30 // 5 minutes (30 * 10 seconds)
            
            Log.d(TAG, "Starting payment verification polling for: $reference")
            
            while (attempts < maxAttempts && isActive) {
                delay(10000) // Wait 10 seconds between checks
                attempts++
                
                try {
                    Log.d(TAG, "Verification attempt $attempts/$maxAttempts for: $reference")
                    
                    val response = ApiService.getApiInterface().verifyPaystackPayment(reference)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val status = response.body()?.data?.status
                        
                        when (status) {
                            "success" -> {
                                Log.d(TAG, "Payment verified successfully: $reference")
                                onSuccess(reference)
                                return@launch
                            }
                            "failed" -> {
                                Log.e(TAG, "Payment failed: $reference")
                                onError("Payment failed. Please try again.")
                                return@launch
                            }
                            "pending" -> {
                                Log.d(TAG, "Payment still pending: $reference")
                                // Continue polling
                            }
                        }
                    } else {
                        Log.w(TAG, "Verification check returned unsuccessful response")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Verification attempt $attempts failed", e)
                    // Continue polling even if one attempt fails
                }
            }
            
            // If we've exhausted all attempts
            if (attempts >= maxAttempts) {
                Log.w(TAG, "Payment verification timeout for: $reference")
                onError("Payment verification timeout. Please check your giving history to confirm payment status.")
            }
        }
    }
    
    /**
     * Cancels the ongoing payment verification polling
     */
    fun cancelVerification() {
        verificationJob?.cancel()
        Log.d(TAG, "Payment verification cancelled")
    }
}
