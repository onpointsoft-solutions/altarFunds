package com.altarfunds.member.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityRegisterBinding
import com.altarfunds.member.models.RegisterRequest
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var progressDialog: ProgressDialog
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupListeners()
        progressDialog = ProgressDialog(this)
        setupInputValidation()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.register)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            if (validateInput()) {
                register()
            }
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }
        
        binding.tvSearchChurch.setOnClickListener {
            val intent = Intent(this, com.altarfunds.member.ui.church.ChurchSearchActivity::class.java)
            intent.putExtra("SELECT_MODE", true)
            startActivityForResult(intent, CHURCH_SEARCH_REQUEST)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHURCH_SEARCH_REQUEST && resultCode == RESULT_OK) {
            val churchCode = data?.getStringExtra("church_code")
            churchCode?.let {
                binding.etChurchCode.setText(it)
                showToast("✓ Church selected: $it")
            } ?: run {
                showToast("✗ Failed to get church code. Please try again.")
            }
        }
    }
    
    companion object {
        private const val CHURCH_SEARCH_REQUEST = 1001
    }
    
    private fun validateInput(): Boolean {
        return InputValidator.validateForm(
            binding.etFirstName to binding.tilFirstName,
            binding.etLastName to binding.tilLastName,
            binding.etEmail to binding.tilEmail,
            binding.etPhone to binding.tilPhone,
            binding.etPassword to binding.tilPassword,
            binding.etConfirmPassword to binding.tilConfirmPassword,
            binding.etChurchCode to binding.tilChurchCode
        )
    }
    
    private fun setupInputValidation() {
        // Email validation
        InputValidator.addEmailValidation(binding.etEmail, binding.tilEmail)
        
        // Phone validation
        InputValidator.addPhoneValidation(binding.etPhone, binding.tilPhone)
        
        // Password validation with confirm password
        InputValidator.addPasswordValidation(
            binding.etPassword, 
            binding.tilPassword,
            binding.etConfirmPassword,
            binding.tilConfirmPassword
        )
        
        // Required field validations
        InputValidator.addRequiredFieldValidation(binding.etFirstName, binding.tilFirstName, "First name")
        InputValidator.addRequiredFieldValidation(binding.etLastName, binding.tilLastName, "Last name")
        InputValidator.addRequiredFieldValidation(binding.etChurchCode, binding.tilChurchCode, "Church code")
    }
    
    private fun register() {
        progressDialog.setMessage("Creating your account...")
        progressDialog.show()
        
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim().formatPhoneNumber()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        
        lifecycleScope.launch {
            try {
                val request = RegisterRequest(
                    email = email,
                    password = password,
                    passwordConfirm = confirmPassword,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phone
                )
                
                val response = app.apiService.register(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val registerResponse = response.body()!!
                    showToast("✓ Registration successful! Please login with your credentials.")
                    
                    binding.btnRegister.animateSuccess {
                        // Navigate back to login
                        finish()
                    }
                } else {
                    // Try to parse error response body for detailed errors
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = when (response.code()) {
                        400 -> {
                            if (errorBody?.contains("email", ignoreCase = true) == true) {
                                "✗ Invalid email format. Please enter a valid email address."
                            } else if (errorBody?.contains("password", ignoreCase = true) == true) {
                                "✗ Password requirements not met. Use at least 8 characters."
                            }
                            else {
                                "✗ Invalid registration data. Please check all fields."
                            }
                        }
                        409 -> "✗ This email is already registered. Please login instead."
                        422 -> "✗ Validation error. Please check all required fields."
                        500 -> "✗ Server error. Please try again later."
                        else -> "✗ Registration failed: ${response.message() ?: "Unknown error"}"
                    }
                    showToast(errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                e.printStackTrace()
                showToast("✗ No internet connection. Please check your network.")
            } catch (e: java.net.SocketTimeoutException) {
                e.printStackTrace()
                showToast("✗ Connection timeout. Please try again.")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ Registration failed: ${e.message ?: "Network error"}")
            } finally {
                progressDialog.dismiss()
            }
        }
    }
}
