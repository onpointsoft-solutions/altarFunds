package com.altarfunds.member.ui.giving

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.altarfunds.member.databinding.ActivityGivingBinding
import com.altarfunds.member.models.*
import com.altarfunds.member.utils.TokenManager
import com.altarfunds.member.utils.ThemeManager
import com.altarfunds.member.viewmodel.GivingViewModel
import com.altarfunds.member.viewmodel.Resource

class EnhancedGivingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGivingBinding
    private lateinit var viewModel: GivingViewModel
    private lateinit var tokenManager: TokenManager
    
    private var selectedCategory: GivingCategory? = null
    private var churchPaymentDetails: ChurchPaymentDetails? = null
    private var churchThemeColors: ChurchThemeColors? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGivingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize components
        tokenManager = TokenManager(this)
        viewModel = ViewModelProvider(this)[GivingViewModel::class.java]
        
        // Initialize API service
        viewModel.initializeApiService(tokenManager)
        
        setupUI()
        loadData()
    }
    
    private fun setupUI() {
        // Setup payment button
        binding.btnGive.setOnClickListener {
            proceedToPayment()
        }
        
        // Initially hide progress bar
        binding.progressBar.visibility = View.GONE
    }
    
    private fun loadData() {
        viewModel.loadGivingCategories()
        viewModel.loadPaymentDetails()
        viewModel.loadThemeColors()
    }
    
    private fun showPaymentOptions(category: GivingCategory) {
        // Update info text with selected category
        binding.tvInfo.text = "Selected: ${category.name}\nYou will receive an M-Pesa prompt on your phone. Enter your PIN to complete the transaction."
        
        // Apply church theme colors if available
        churchThemeColors?.let { colors ->
            ThemeManager.applyChurchTheme(this, colors)
        }
    }
    
    private fun proceedToPayment() {
        val amount = binding.etAmount.text.toString()
        if (amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        val amountValue = amount.toDouble()
        val categoryId = selectedCategory?.id ?: 1
        
        // Show loading
        showLoading()
        binding.tvInfo.text = "Initiating payment..."
        
        // Let backend handle payment initiation based on user's preferred method
        // Backend will determine payment method (M-Pesa, Paystack, etc.) and handle integration
        viewModel.initiatePayment(
            amount = amountValue,
            categoryId = categoryId,
            paymentMethod = "mpesa", // Backend can override based on user preferences/availability
            phoneNumber = getUserPhoneNumber(), // Get from user profile
            email = getUserEmail() // Get from user profile
        )
        
        // Observe the result
        observeDonationResult()
    }
    
    private fun observeDonationResult() {
        viewModel.donationResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading()
                    binding.tvInfo.text = "Processing payment..."
                }
                is Resource.Success -> {
                    val donation = resource.data
                    if (donation.paymentStatus == "pending") {
                        binding.tvInfo.text = "Payment initiated!\nReference: ${donation.paymentReference ?: donation.transactionId}\nPlease complete payment on your device."
                        
                        // Start polling for payment status
                        startPaymentStatusCheck(donation.id)
                    } else if (donation.paymentStatus == "completed") {
                        showPaymentSuccess()
                    } else {
                        showPaymentError("Payment status: ${donation.paymentStatus}")
                    }
                }
                is Resource.Error -> {
                    showPaymentError(resource.message)
                }
            }
        }
    }
    
    private fun startPaymentStatusCheck(donationId: Int) {
        // Poll for payment status every 5 seconds
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                viewModel.checkPaymentStatus(donationId)
                handler.postDelayed(this, 5000) // Check every 5 seconds
            }
        }
        handler.post(runnable)
        
        // Stop polling after 2 minutes
        handler.postDelayed({
            handler.removeCallbacks(runnable)
            if (viewModel.donationResult.value is Resource.Success) {
                val donation = (viewModel.donationResult.value as Resource.Success).data
                if (donation.paymentStatus == "pending") {
                    showPaymentTimeout()
                }
            }
        }, 120000) // 2 minutes timeout
    }
    
    private fun showPaymentSuccess() {
        hideLoading()
        binding.tvInfo.text = "Payment Successful!\nThank you for your giving."
        binding.btnGive.visibility = View.GONE
        
        // Reset form after delay
        binding.root.postDelayed({
            finish()
        }, 3000)
    }
    
    private fun showPaymentError(message: String) {
        hideLoading()
        binding.tvInfo.text = "Payment Failed: $message\nPlease try again."
        binding.btnGive.visibility = View.VISIBLE
    }
    
    private fun showPaymentTimeout() {
        hideLoading()
        binding.tvInfo.text = "Payment timed out.\nPlease check if payment was completed and contact support if needed."
        binding.btnGive.visibility = View.VISIBLE
    }
    
    private fun getUserPhoneNumber(): String? {
        // TODO: Get phone number from user profile
        return null // Backend can use church member's phone number
    }
    
    private fun getUserEmail(): String? {
        // TODO: Get email from user profile
        return null // Backend can use church member's email
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGive.visibility = View.GONE
    }
    
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnGive.visibility = View.VISIBLE
    }
    
    override fun onResume() {
        super.onResume()
        // Reload data when activity resumes
        loadData()
    }
}
