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
        
        // For now, use a default category
        val defaultCategory = GivingCategory(1, "General", "General giving", true, 0, false, null, null)
        showPaymentOptions(defaultCategory)
    }
    
    private fun initiateCardPayment() {
        val amount = binding.etAmount.text.toString()
        if (amount.isEmpty()) {
            Toast.makeText(this, "Please enter amount first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val amountValue = amount.toDouble()
        val categoryId = selectedCategory?.id ?: 1
        
        // Create donation through backend
        viewModel.createDonation(categoryId, amountValue, "card")
        
        Toast.makeText(this, "Processing payment of KES $amountValue...", Toast.LENGTH_SHORT).show()
        
        // For now, simulate success - backend will handle actual payment processing
        showPaymentSuccess()
    }
    
    private fun showPaymentSuccess() {
        // Show success message in info text
        binding.tvInfo.text = "Payment Successful!\nThank you for your giving."
        
        // Hide button and show progress
        binding.btnGive.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        
        // Reset form after delay
        binding.root.postDelayed({
            finish()
        }, 3000)
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
