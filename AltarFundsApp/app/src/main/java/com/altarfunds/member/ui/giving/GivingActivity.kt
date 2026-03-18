package com.altarfunds.member.ui.giving

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityGivingBinding
import com.altarfunds.member.models.GivingCategory
import com.altarfunds.member.models.MpesaRequest
import com.altarfunds.member.utils.*
import com.altarfunds.member.viewmodel.GivingViewModel
import com.altarfunds.member.viewmodel.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GivingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGivingBinding
    private val app by lazy { MemberApp.getInstance() }
    private val viewModel: GivingViewModel by lazy { ViewModelProvider(this)[GivingViewModel::class.java] }
    private lateinit var progressDialog: ProgressDialog
    private var categories: List<GivingCategory> = emptyList()
    private var selectedCategoryId: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGivingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        progressDialog = ProgressDialog(this)
    }

    private fun setupUI() {
        setupPaymentMethodSpinner()
        loadGivingCategories()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.give_now)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadGivingCategories() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getGivingCategories()

                if (response.isSuccessful && response.body() != null) {
                    categories = response.body()!!.data ?: emptyList()
                    setupCategorySpinner()
                } else {
                    // Fallback to default categories if API fails
                    setupDefaultCategories()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                setupDefaultCategories()
            }
        }
    }

    private fun setupCategorySpinner() {
        val categoryNames = categories.map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter

        binding.spinnerType.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedCategoryId = categories[position].id
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    private fun setupPaymentMethodSpinner() {
        val paymentMethods = arrayOf("M-Pesa", "Card")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentMethods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPaymentMethod.adapter = adapter
    }

    private fun setupDefaultCategories() {
        val defaultTypes = arrayOf("Tithe", "Offering", "Building Fund", "Mission", "Special")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, defaultTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnGive.setOnClickListener {
            if (validateInput()) {
                val paymentMethod = binding.spinnerPaymentMethod.selectedItem.toString()
                when (paymentMethod) {
                    "M-Pesa" -> initiateMpesaPayment()
                    "Card" -> initiatePaystackPayment()
                    else -> showToast("Payment method not supported")
                }
            }
        }
    }
    
    private fun validateInput(): Boolean {
        val amount = binding.etAmount.text.toString()
        val phone = binding.etPhone.text.toString()
        val paymentMethod = binding.spinnerPaymentMethod.selectedItem.toString()
        
        if (amount.isEmpty()) {
            binding.tilAmount.error = "Amount is required"
            return false
        }
        
        if (amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            binding.tilAmount.error = "Invalid amount"
            return false
        }
        
        // Phone number only required for M-Pesa
        if (paymentMethod == "M-Pesa" && phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required for M-Pesa"
            return false
        }
        
        if (paymentMethod == "M-Pesa" && !phone.isValidPhone()) {
            binding.tilPhone.error = "Invalid phone number"
            return false
        }
        
        binding.tilAmount.error = null
        binding.tilPhone.error = null
        return true
    }
    
    private fun initiateMpesaPayment() {
        progressDialog.setMessage("Initiating M-Pesa payment...")
        progressDialog.show()
        
        val amount = binding.etAmount.text.toString()
        val phone = binding.etPhone.text.toString().formatPhoneNumber()
        val description = binding.etDescription.text.toString().ifEmpty { "Donation" }
        
        lifecycleScope.launch {
            try {
                val request = MpesaRequest(
                    phoneNumber = phone,
                    amount = amount,
                    donationType = selectedCategoryId.toString(),
                    description = description
                )
                
                val response = app.apiService.initiateMpesaPayment(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val mpesaResponse = response.body()!!
                    showToast(mpesaResponse.customerMessage)
                    
                    binding.btnGive.animateSuccess {
                        checkPaymentStatus(mpesaResponse.checkoutRequestId)
                    ErrorHandler.showError(this@GivingActivity, response.code(), "Failed to initiate M-Pesa payment")
                    progressDialog.dismiss()
                }
            } }
            catch (e: Exception) {
                e.printStackTrace()
                ErrorHandler.showError(this@GivingActivity, e)
                progressDialog.dismiss()
            }
        }
    }
    
    private fun initiatePaystackPayment() {
        val amount = binding.etAmount.text.toString()
        val selectedCategory = binding.spinnerType.selectedItem as? GivingCategory
        
        if (selectedCategory == null) {
            showToast("Please select a giving category")
            return
        }
        
        viewModel.initializePaystackPayment(
            amount = amount,
            givingType = selectedCategory.name,
            churchId = app.tokenManager.getChurchId() ?: 1,
            email = app.tokenManager.getUserEmail() ?: ""
        )
        
        viewModel.paystackPaymentResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    progressDialog.setMessage("Initializing card payment...")
                    progressDialog.show()
                }
                is Resource.Success<*> -> {
                    progressDialog.dismiss()
                    binding.btnGive.animateSuccess {
                        // Open Paystack payment URL in browser
                        openPaystackPayment(result.data as String)
                    }
                }
                is Resource.Error -> {
                    progressDialog.dismiss()
                    showToast(result.message)
                }
            }
        }
    }
    
    private fun openPaystackPayment(authorizationUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl))
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Could not open payment page")
        }
    }
    
    private fun checkPaymentStatus(checkoutRequestId: String) {
        lifecycleScope.launch {
            delay(15000)
            
            try {
                val response = app.apiService.checkPaymentStatus(checkoutRequestId)
                
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    showToast(status.message)
                    
                    if (status.status == "completed") {
                        finish()
                    }
                } else {
                    showToast("Could not verify payment status")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorHandler.showError(this@GivingActivity, e)
            }
        }
    }
    }
