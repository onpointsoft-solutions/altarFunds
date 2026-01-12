package com.altarfunds.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.mobile.databinding.ActivityPaymentMethodBinding

class PaymentMethodActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentMethodBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentMethodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupPaymentMethods()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Payment Method"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupPaymentMethods() {
        // Setup payment method options
        binding.cardMpesa.setOnClickListener {
            selectPaymentMethod("mpesa")
        }
        
        binding.cardCard.setOnClickListener {
            selectPaymentMethod("card")
        }
        
        binding.cardBank.setOnClickListener {
            selectPaymentMethod("bank")
        }
        
        binding.cardCash.setOnClickListener {
            selectPaymentMethod("cash")
        }
    }

    private fun selectPaymentMethod(method: String) {
        // Reset all cards
        resetCardSelections()
        
        // Highlight selected card
        when (method) {
            "mpesa" -> binding.cardMpesa.strokeColor = getColor(com.google.android.material.R.color.design_default_color_primary)
            "card" -> binding.cardCard.strokeColor = getColor(com.google.android.material.R.color.design_default_color_primary)
            "bank" -> binding.cardBank.strokeColor = getColor(com.google.android.material.R.color.design_default_color_primary)
            "cash" -> binding.cardCash.strokeColor = getColor(com.google.android.material.R.color.design_default_color_primary)
        }
        
        Toast.makeText(this, "Selected: $method", Toast.LENGTH_SHORT).show()
    }

    private fun resetCardSelections() {
        binding.cardMpesa.strokeColor = getColor(android.R.color.transparent)
        binding.cardCard.strokeColor = getColor(android.R.color.transparent)
        binding.cardBank.strokeColor = getColor(android.R.color.transparent)
        binding.cardCash.strokeColor = getColor(android.R.color.transparent)
    }

    private fun setupClickListeners() {
        binding.btnContinue.setOnClickListener {
            // TODO: Process payment method selection
            Toast.makeText(this, "Proceeding with payment", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
}
