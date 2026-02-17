package com.altarfunds.member.ui.giving

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityGivingBinding
import com.altarfunds.member.models.MpesaRequest
import com.altarfunds.member.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GivingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGivingBinding
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGivingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupDonationTypeSpinner()
        setupListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.give_now)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupDonationTypeSpinner() {
        val types = arrayOf("Tithe", "Offering", "Special Offering", "Building Fund", "Mission")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }
    
    private fun setupListeners() {
        binding.btnGive.setOnClickListener {
            if (validateInput()) {
                initiateMpesaPayment()
            }
        }
    }
    
    private fun validateInput(): Boolean {
        val amount = binding.etAmount.text.toString()
        val phone = binding.etPhone.text.toString()
        
        if (amount.isEmpty()) {
            binding.tilAmount.error = "Amount is required"
            return false
        }
        
        if (amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            binding.tilAmount.error = "Invalid amount"
            return false
        }
        
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            return false
        }
        
        if (!phone.isValidPhone()) {
            binding.tilPhone.error = "Invalid phone number"
            return false
        }
        
        binding.tilAmount.error = null
        binding.tilPhone.error = null
        return true
    }
    
    private fun initiateMpesaPayment() {
        showLoading(true)
        
        val amount = binding.etAmount.text.toString()
        val phone = binding.etPhone.text.toString().formatPhoneNumber()
        val donationType = getDonationType()
        val description = binding.etDescription.text.toString().ifEmpty { "Donation" }
        
        lifecycleScope.launch {
            try {
                val request = MpesaRequest(
                    phoneNumber = phone,
                    amount = amount,
                    donationType = donationType,
                    description = description
                )
                
                val response = app.apiService.initiateMpesaPayment(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val mpesaResponse = response.body()!!
                    showToast(mpesaResponse.customerMessage)
                    
                    checkPaymentStatus(mpesaResponse.checkoutRequestId)
                } else {
                    showToast("Failed to initiate payment")
                    showLoading(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(getString(R.string.network_error))
                showLoading(false)
            }
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
                showToast("Could not verify payment status")
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun getDonationType(): String {
        return when (binding.spinnerType.selectedItemPosition) {
            0 -> "tithe"
            1 -> "offering"
            2 -> "special_offering"
            3 -> "building_fund"
            4 -> "mission"
            else -> "offering"
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            binding.btnGive.isEnabled = false
            binding.progressBar.visible()
        } else {
            binding.btnGive.isEnabled = true
            binding.progressBar.gone()
        }
    }
}
