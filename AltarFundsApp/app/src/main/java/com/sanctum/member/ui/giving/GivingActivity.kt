package com.sanctum.member.ui.giving

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import com.sanctum.member.databinding.ActivityGivingBinding
import com.sanctum.member.models.GivingCategory
import com.sanctum.member.utils.*
import com.sanctum.member.viewmodel.GivingViewModel
import com.sanctum.member.viewmodel.Resource
import kotlinx.coroutines.launch

class GivingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGivingBinding
    private val app by lazy { MemberApp.getInstance() }
    private val viewModel: GivingViewModel by lazy {
        ViewModelProvider(this)[GivingViewModel::class.java]
    }
    private lateinit var progressDialog: ProgressDialog
    private var categories: List<GivingCategory> = emptyList()
    private var selectedCategoryId: Int = 0

    // Track whether we've already launched the WebView so the observer
    // doesn't fire again when the Activity resumes.
    private var webViewLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGivingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressDialog = ProgressDialog(this)
        setupToolbar()
        setupPaymentMethodSpinner()
        loadGivingCategories()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.give_now)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ── Observe once ─────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.paystackAuthUrl.observe(this) { result ->
            when (result) {
                is Resource.Loading -> Unit   // progress already shown
                is Resource.Success -> {
                    if (!webViewLaunched) {
                        webViewLaunched = true
                        progressDialog.dismiss()
                        val url  = result.data
                        val txData = (viewModel.donationResult.value as? Resource.Success)?.data
                        val txId   = txData?.transactionId ?: ""
                        val ref    = txData?.paymentReference ?: ""
                        paymentResultLauncher.launch(
                            PaystackWebViewActivity.newIntent(
                                context   = this,
                                authUrl   = url,
                                reference = ref,
                                txId      = txId,
                            )
                        )
                    }
                }
                is Resource.Error -> {
                    progressDialog.dismiss()
                    showToast("Payment init failed: ${result.message}")
                }
            }
        }
    }

    // ── Result launcher ───────────────────────────────────────────────────

    private val paymentResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        webViewLaunched = false   // reset so the next Give works
        if (result.resultCode == RESULT_OK) {
            val paid = result.data
                ?.getBooleanExtra(PaystackWebViewActivity.RESULT_PAID, false) ?: false
            if (paid) {
                showToast("\u2713 Payment completed successfully!")
                finish()
            }
        }
    }

    // ── Category loading ──────────────────────────────────────────────────

    private fun loadGivingCategories() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getGivingCategories()
                if (response.isSuccessful && response.body() != null) {
                    categories = response.body()!!.data ?: emptyList()
                    setupCategorySpinner()
                } else {
                    setupDefaultCategories()
                }
            } catch (e: Exception) {
                setupDefaultCategories()
            }
        }
    }

    private fun setupCategorySpinner() {
        val names = categories.map { it.name }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
        binding.spinnerType.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    selectedCategoryId = categories[pos].id
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
    }

    private fun setupDefaultCategories() {
        val defaults = arrayOf("Tithe", "Offering", "Building Fund", "Mission", "Special")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, defaults)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupPaymentMethodSpinner() {
        val methods = arrayOf("M-Pesa", "Card")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, methods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPaymentMethod.adapter = adapter
    }

    // ── Listeners ─────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnGive.setOnClickListener {
            if (validateInput()) initiatePaystackPayment()
        }
    }

    // ── Validation ────────────────────────────────────────────────────────

    private fun validateInput(): Boolean {
        val amount = binding.etAmount.text.toString().trim()
        val phone  = binding.etPhone.text.toString().trim()
        val method = binding.spinnerPaymentMethod.selectedItem.toString()

        if (amount.isEmpty() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            binding.tilAmount.error = "Enter a valid amount"
            return false
        }
        binding.tilAmount.error = null

        if (method == "M-Pesa" && phone.isEmpty()) {
            binding.tilPhone.error = "Phone number required for M-Pesa"
            return false
        }
        if (method == "M-Pesa" && !phone.isValidPhone()) {
            binding.tilPhone.error = "Invalid phone number"
            return false
        }
        binding.tilPhone.error = null

        if (categories.isNotEmpty() && selectedCategoryId <= 0) {
            showToast("Please select a giving category")
            return false
        }
        return true
    }

    // ── Payment — all methods go through Paystack WebView ─────────────────

    private fun initiatePaystackPayment() {
        val amount     = binding.etAmount.text.toString().trim().toDouble()
        val email      = app.tokenManager.getUserEmail() ?: ""
        val categoryId = if (categories.isNotEmpty()) selectedCategoryId
                         else binding.spinnerType.selectedItemPosition + 1

        webViewLaunched = false
        progressDialog.setMessage("Initializing payment\u2026")
        progressDialog.show()

        viewModel.initiateCardPayment(
            amount     = amount,
            categoryId = categoryId,
            email      = email,
        )
    }
}
