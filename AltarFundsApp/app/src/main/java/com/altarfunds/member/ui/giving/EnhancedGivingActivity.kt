package com.altarfunds.member.ui.giving

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityGivingBinding
import com.altarfunds.member.adapters.GivingCategoryAdapter
import com.altarfunds.member.models.*
import com.altarfunds.member.ui.BaseActivity
import com.altarfunds.member.utils.TokenManager
import com.altarfunds.member.utils.ThemeManager
import com.altarfunds.member.viewmodel.GivingViewModel
import co.paystack.android.Paystack
import co.paystack.android.PaystackSdk
import co.paystack.android.Transaction
import co.paystack.android.model.Charge
import co.paystack.android.model.ChargeCard
import co.paystack.android.model.Emergency

class EnhancedGivingActivity : BaseActivity() {
    
    private lateinit var binding: ActivityGivingBinding
    private lateinit var viewModel: GivingViewModel
    private lateinit var categoryAdapter: GivingCategoryAdapter
    private lateinit var tokenManager: TokenManager
    
    private var selectedCategory: GivingCategory? = null
    private var churchPaymentDetails: ChurchPaymentDetails? = null
    private var churchThemeColors: ChurchThemeColors? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGivingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize Paystack
        PaystackSdk.initialize(applicationContext)
        
        // Initialize components
        tokenManager = TokenManager(this)
        viewModel = ViewModelProvider(this)[GivingViewModel::class.java]
        
        setupUI()
        observeViewModel()
        loadData()
    }
    
    private fun setupUI() {
        // Setup RecyclerView
        categoryAdapter = GivingCategoryAdapter { category ->
            selectedCategory = category
            showPaymentOptions(category)
        }
        
        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(this@EnhancedGivingActivity)
            adapter = categoryAdapter
        }
        
        // Setup click listeners
        binding.buttonProceed.setOnClickListener {
            selectedCategory?.let { proceedToPayment(it) }
        }
        
        binding.buttonCardPayment.setOnClickListener {
            initiateCardPayment()
        }
        
        // Apply theme colors if available
        churchThemeColors?.let { theme ->
            ThemeManager.applyChurchTheme(this, theme)
            // Apply specific theme to giving UI elements
            applyGivingTheme(theme)
        }
    }
    
    private fun observeViewModel() {
        viewModel.givingCategories.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    hideLoading()
                    categoryAdapter.submitList(result.data ?: emptyList())
                }
                is Resource.Error -> {
                    hideLoading()
                    showError(result.message)
                }
                is Resource.Loading -> {
                    showLoading()
                }
            }
        }
        
        viewModel.paymentDetails.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    churchPaymentDetails = result.data
                    updatePaymentUI()
                }
                is Resource.Error -> {
                    showError(result.message)
                }
                else -> {}
            }
        }
        
        viewModel.themeColors.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    churchThemeColors = result.data
                    applyThemeColors(result.data)
                }
                is Resource.Error -> {
                    // Continue with default theme
                }
                else -> {}
            }
        }
        
        viewModel.paymentResult.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    hideLoading()
                    val paymentData = result.data
                    val paymentUrl = paymentData?.get("payment_url") as? String
                    if (!paymentUrl.isNullOrEmpty()) {
                        launchPaystackPayment(paymentUrl)
                    } else {
                        showError("Failed to get payment URL")
                    }
                }
                is Resource.Error -> {
                    hideLoading()
                    showError(result.message)
                }
                is Resource.Loading -> {
                    showLoading("Initializing payment...")
                }
            }
        }
        
        viewModel.donationResult.observe(this) { result ->
            when (result) {
                is Resource.Success -> {
                    hideLoading()
                    showPaymentSuccess(result.data)
                }
                is Resource.Error -> {
                    hideLoading()
                    showError(result.message)
                }
                is Resource.Loading -> {
                    showLoading()
                }
            }
        }
    }
    
    private fun loadData() {
        viewModel.loadGivingCategories()
        viewModel.loadPaymentDetails()
        viewModel.loadThemeColors()
    }
    
    private fun showPaymentOptions(category: GivingCategory) {
        binding.layoutPaymentOptions.visibility = View.VISIBLE
        binding.textViewSelectedCategory.text = category.name
        
        // Only show Paystack payment - fully automated
        binding.buttonCardPayment.visibility = View.VISIBLE
        binding.buttonMpesaPayment.visibility = View.GONE
        binding.buttonBankPayment.visibility = View.GONE
        binding.textViewChurchPaymentInfo.visibility = View.GONE
        
        // Directly initiate Paystack payment
        initiateCardPayment()
    }
    
    private fun proceedToPayment(category: GivingCategory) {
        val amount = binding.editTextAmount.text.toString()
        if (amount.isEmpty()) {
            binding.editTextAmount.error = "Please enter amount"
            return
        }
        
        binding.layoutPaymentOptions.visibility = View.VISIBLE
        showPaymentOptions(category)
    }
    
        
    private fun initiateCardPayment() {
        val amount = binding.editTextAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Initialize payment via backend API
        initializePaymentViaBackend(amount)
    }
    
    private fun initializePaymentViaBackend(amount: Double) {
        showLoading("Initializing payment...")
        
        viewModel.initializePayment(
            categoryId = selectedCategory?.id ?: 0,
            amount = amount
        )
    }
    
    private fun launchPaystackPayment(paymentUrl: String) {
        hideLoading()
        
        // Launch Paystack payment in a web view or browser
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(paymentUrl))
        startActivity(intent)
        
        // Show payment processing message
        showPaymentDialog("Payment Initiated", 
            "You've been redirected to Paystack to complete your payment securely.\n\n" +
            "After successful payment, you'll be redirected back to the app.\n" +
            "Amount: KES ${binding.editTextAmount.text}\n" +
            "Category: ${selectedCategory?.name}"
        )
    }
    
    private fun simulateCardPayment(charge: Charge) {
        showLoading("Processing card payment...")
        
        // In a real implementation, you would use Paystack's card payment UI
        // For demonstration, we'll simulate the process
        viewModel.createDonation(
            categoryId = selectedCategory?.id ?: 0,
            amount = binding.editTextAmount.text.toString().toDouble(),
            paymentMethod = "card",
            reference = charge.reference
        )
    }
    
    private fun showPaymentSuccess(donation: Donation) {
        val message = buildString {
            append("Payment Successful!\n\n")
            append("Transaction ID: ${donation.id}\n")
            append("Amount: KES ${donation.amount}\n")
            append("Category: ${selectedCategory?.name}\n")
            append("Status: ${donation.status}\n\n")
            append("Thank you for your generous donation!")
        }
        
        showPaymentDialog("Payment Successful", message) {
            // Navigate back or to donation history
            finish()
        }
    }
    
    private fun applyGivingTheme(theme: ChurchThemeColors) {
        try {
            // Apply primary color to main elements
            ThemeManager.applyThemeToView(binding.buttonProceed, "primary")
            ThemeManager.applyThemeToView(binding.buttonCardPayment, "primary")
            
            // Update app name if different
            if (title != theme.church_name) {
                title = "${theme.church_name} - Giving"
            }
            
        } catch (e: Exception) {
            // If theme application fails, use default colors
        }
    }
    
    private fun updatePaymentUI() {
        churchPaymentDetails?.let { details ->
            if (!details.allow_online_giving) {
                binding.layoutPaymentOptions.visibility = View.GONE
                binding.textViewPaymentDisabled.visibility = View.VISIBLE
                binding.textViewPaymentDisabled.text = 
                    "Online payments are currently disabled. Please contact your church administrator."
            }
        }
    }
    
    private fun showPaymentDialog(title: String, message: String, onDismiss: (() -> Unit)? = null) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
            .setCancelable(true)
            .show()
    }
    
    private fun showLoading(message: String = "Loading...") {
        binding.progressBar.visibility = View.VISIBLE
        binding.textViewLoading.text = message
        binding.textViewLoading.visibility = View.VISIBLE
    }
    
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.textViewLoading.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
