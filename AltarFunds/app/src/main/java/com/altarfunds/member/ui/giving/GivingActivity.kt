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

    // How many times to poll for payment status before giving up
    private val MAX_POLL_ATTEMPTS  = 10
    // Seconds to wait between each poll
    private val POLL_INTERVAL_MS   = 5_000L
    // Initial wait before the first poll (STK push takes a few seconds to process)
    private val INITIAL_DELAY_MS   = 10_000L

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
            if (validateInput()) initiateMpesaPayment()
        }
    }

    // ── Validation ────────────────────────────────────────────────────────

    private fun validateInput(): Boolean {
        val amount = binding.etAmount.text.toString().trim()
        val phone  = binding.etPhone.text.toString().trim()
        var valid  = true

        if (amount.isEmpty()) {
            binding.tilAmount.error = "Amount is required"
            valid = false
        } else if (amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            binding.tilAmount.error = "Enter a valid amount"
            valid = false
        } else {
            binding.tilAmount.error = null
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            valid = false
        } else if (!phone.isValidPhone()) {
            binding.tilPhone.error = "Enter a valid phone number (e.g. 0712345678)"
            valid = false
        } else {
            binding.tilPhone.error = null
        }

        return valid
    }

    // ── M-Pesa STK push ───────────────────────────────────────────────────

    private fun initiateMpesaPayment() {
        showLoading(true, "Sending payment request…")

        val amount      = binding.etAmount.text.toString().trim()
        val phone       = binding.etPhone.text.toString().trim().formatPhoneNumber()
        val donationType = getDonationType()
        val description = binding.etDescription.text.toString().trim().ifEmpty { "Donation" }

        lifecycleScope.launch {
            try {
                val request = MpesaRequest(
                    phoneNumber  = phone,
                    amount       = amount,
                    donationType = donationType,
                    description  = description
                )

                val response = app.apiService.initiateMpesaPayment(request)

                if (response.isSuccessful && response.body() != null) {
                    val mpesaResponse = response.body()!!

                    // Inform the user to check their phone
                    showToast(mpesaResponse.customerMessage)
                    showLoading(true, "Waiting for M-Pesa confirmation…")

                    // Poll for status
                    pollPaymentStatus(mpesaResponse.checkoutRequestId)

                } else {
                    val msg = when (response.code()) {
                        400  -> "✗ Invalid payment details. Please check and try again."
                        401  -> "✗ Session expired. Please log in again."
                        402  -> "✗ Insufficient funds."
                        500  -> "✗ Server error. Please try again later."
                        else -> "✗ Failed to initiate payment (${response.code()})."
                    }
                    showToast(msg)
                    showLoading(false)
                }

            } catch (e: java.net.UnknownHostException) {
                showToast("✗ No internet connection.")
                showLoading(false)
            } catch (e: java.net.SocketTimeoutException) {
                showToast("✗ Connection timed out. Please try again.")
                showLoading(false)
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ ${e.message ?: getString(R.string.network_error)}")
                showLoading(false)
            }
        }
    }

    // ── Payment status polling ─────────────────────────────────────────────

    /**
     * Polls the payment status endpoint up to [MAX_POLL_ATTEMPTS] times,
     * waiting [POLL_INTERVAL_MS] between each attempt. Starts after an
     * [INITIAL_DELAY_MS] wait for the STK push to be processed by Safaricom.
     */
    private suspend fun pollPaymentStatus(checkoutRequestId: String) {
        delay(INITIAL_DELAY_MS)

        for (attempt in 1..MAX_POLL_ATTEMPTS) {
            try {
                val response = app.apiService.checkPaymentStatus(checkoutRequestId)

                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!

                    when (status.status.lowercase()) {
                        "completed", "success" -> {
                            showLoading(false)
                            showToast("✓ ${status.message}")
                            // Brief pause so the toast is visible, then close
                            delay(1500)
                            setResult(RESULT_OK)
                            finish()
                            return
                        }
                        "failed", "cancelled" -> {
                            showLoading(false)
                            showToast("✗ ${status.message}")
                            return
                        }
                        else -> {
                            // Still pending — update progress message and wait
                            val remaining = MAX_POLL_ATTEMPTS - attempt
                            showLoading(true, "Confirming payment… ($remaining checks left)")
                        }
                    }
                } else if (response.code() == 404) {
                    // Transaction not found yet — still processing
                    val remaining = MAX_POLL_ATTEMPTS - attempt
                    showLoading(true, "Confirming payment… ($remaining checks left)")
                } else {
                    // Unexpected error during polling — keep trying
                }

            } catch (e: Exception) {
                // Network hiccup during polling — keep trying
            }

            if (attempt < MAX_POLL_ATTEMPTS) delay(POLL_INTERVAL_MS)
        }

        // Exhausted all attempts without a definitive result
        showLoading(false)
        showToast("Could not confirm payment. Check M-Pesa messages for confirmation.")
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun getDonationType(): String = when (binding.spinnerType.selectedItemPosition) {
        0    -> "tithe"
        1    -> "offering"
        2    -> "special_offering"
        3    -> "building_fund"
        4    -> "mission"
        else -> "offering"
    }

    private fun showLoading(show: Boolean, message: String = "") {
        runOnUiThread {
            if (show) {
                binding.btnGive.isEnabled = false
                binding.progressBar.visible()
                if (message.isNotEmpty()) showToast(message)
            } else {
                binding.btnGive.isEnabled = true
                binding.progressBar.gone()
            }
        }
    }
}
