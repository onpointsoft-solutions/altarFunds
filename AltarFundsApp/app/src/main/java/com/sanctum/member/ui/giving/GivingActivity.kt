package com.sanctum.member.ui.giving

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import com.sanctum.member.databinding.ActivityGivingBinding
import com.sanctum.member.models.GivingCategory
import com.sanctum.member.models.MpesaRequest
import com.sanctum.member.utils.*
import com.sanctum.member.viewmodel.GivingViewModel
import com.sanctum.member.viewmodel.Resource
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
        val amount = binding.etAmount.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val paymentMethod = binding.spinnerPaymentMethod.selectedItem.toString()

        if (amount.isEmpty()) {
            binding.tilAmount.error = "Amount is required"
            return false
        }

        if (amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            binding.tilAmount.error = "Invalid amount"
            return false
        }

        binding.tilAmount.error = null

        // Phone number only required for M-Pesa
        if (paymentMethod == "M-Pesa" && phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required for M-Pesa"
            return false
        }

        if (paymentMethod == "M-Pesa" && !phone.isValidPhone()) {
            binding.tilPhone.error = "Invalid phone number"
            return false
        }

        binding.tilPhone.error = null

        // Category check — selectedCategoryId is set by onItemSelected
        // and will be > 0 when the API categories loaded successfully.
        // When using default string categories (API fallback), position 0
        // is still a valid choice so we skip the ID check in that case.
        if (categories.isNotEmpty() && selectedCategoryId <= 0) {
            showToast("Please select a giving category")
            return false
        }

        return true
    }

    private fun initiateMpesaPayment() {
        progressDialog.setMessage("Initiating M-Pesa payment...")
        progressDialog.show()

        val amount = binding.etAmount.text.toString().trim()
        val phone  = binding.etPhone.text.toString().trim().formatPhoneNumber()

        // selectedCategoryId is set by onItemSelected — safe to use directly
        // For default (fallback) string categories, use position-based ID
        val categoryId = if (categories.isNotEmpty()) {
            selectedCategoryId
        } else {
            binding.spinnerType.selectedItemPosition + 1  // 1-based fallback
        }

        viewModel.initiatePayment(
            amount        = amount.toDouble(),
            categoryId    = categoryId,
            paymentMethod = "mpesa",
            phoneNumber   = phone,
            email         = app.tokenManager.getUserEmail()
        )

        viewModel.donationResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> { /* already showing progress */ }
                is Resource.Success -> {
                    progressDialog.dismiss()
                    val donation = result.data
                    when (donation.paymentStatus) {
                        "pending"   -> {
                            showToast("M-Pesa payment initiated! Check your phone for STK prompt.")
                            startPaymentStatusCheck(donation.id)
                        }
                        "completed" -> {
                            binding.btnGive.animateSuccess {
                                showToast("✓ Payment successful!")
                                finish()
                            }
                        }
                        else -> showToast("Payment status: ${donation.paymentStatus}")
                    }
                }
                is Resource.Error -> {
                    progressDialog.dismiss()
                    showToast(result.message)
                }
            }
        }
    }

    private fun initiatePaystackPayment() {
        val amount = binding.etAmount.text.toString().trim()

        val categoryId = if (categories.isNotEmpty()) {
            selectedCategoryId
        } else {
            binding.spinnerType.selectedItemPosition + 1
        }

        viewModel.initiatePayment(
            amount        = amount.toDouble(),
            categoryId    = categoryId,
            paymentMethod = "card",
            phoneNumber   = null,
            email         = app.tokenManager.getUserEmail()
        )

        viewModel.donationResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {
                    progressDialog.setMessage("Processing payment...")
                    progressDialog.show()
                }
                is Resource.Success -> {
                    progressDialog.dismiss()
                    val donation = result.data
                    when (donation.paymentStatus) {
                        "pending"   -> {
                            showToast("Payment initiated! Reference: ${donation.paymentReference ?: donation.transactionId}")
                            startPaymentStatusCheck(donation.id)
                        }
                        "completed" -> {
                            binding.btnGive.animateSuccess {
                                showToast("✓ Payment successful!")
                                finish()
                            }
                        }
                        else -> showToast("Payment status: ${donation.paymentStatus}")
                    }
                }
                is Resource.Error -> {
                    progressDialog.dismiss()
                    showToast(result.message)
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
            runOnUiThread {
                showToast("Payment verification timed out. Please check if payment was completed.")
            }
        }, 120000) // 2 minutes timeout
    }
}