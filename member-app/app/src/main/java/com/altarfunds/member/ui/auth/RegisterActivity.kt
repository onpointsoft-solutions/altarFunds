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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupListeners()
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
    }
    
    private fun validateInput(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val churchCode = binding.etChurchCode.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        
        // Clear previous errors
        binding.tilFirstName.error = null
        binding.tilLastName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilChurchCode.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        
        // Validate first name
        if (firstName.isEmpty()) {
            binding.tilFirstName.error = getString(R.string.required_field)
            return false
        }
        
        // Validate last name
        if (lastName.isEmpty()) {
            binding.tilLastName.error = getString(R.string.required_field)
            return false
        }
        
        // Validate email
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.required_field)
            return false
        }
        
        if (!email.isValidEmail()) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            return false
        }
        
        // Validate phone
        if (phone.isEmpty()) {
            binding.tilPhone.error = getString(R.string.required_field)
            return false
        }
        
        if (!phone.isValidPhone()) {
            binding.tilPhone.error = getString(R.string.invalid_phone)
            return false
        }
        
        // Validate church code
        if (churchCode.isEmpty()) {
            binding.tilChurchCode.error = getString(R.string.required_field)
            return false
        }
        
        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.required_field)
            return false
        }
        
        if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.password_too_short)
            return false
        }
        
        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.required_field)
            return false
        }
        
        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.password_mismatch)
            return false
        }
        
        return true
    }
    
    private fun register() {
        showLoading(true)
        
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
                    showToast(getString(R.string.registration_successful))
                    
                    // Navigate back to login
                    finish()
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Invalid registration data. Please check your inputs."
                        409 -> "Email already exists"
                        else -> response.message() ?: getString(R.string.error_occurred)
                    }
                    showToast(errorMessage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(getString(R.string.network_error))
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            binding.progressBar.visible()
            binding.btnRegister.isEnabled = false
        } else {
            binding.progressBar.gone()
            binding.btnRegister.isEnabled = true
        }
    }
}
