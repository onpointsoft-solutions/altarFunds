package com.altarfunds.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.databinding.ActivityLoginBinding
import com.altarfunds.mobile.utils.DeviceUtils
import com.altarfunds.mobile.utils.ValidationUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if already logged in
        if (isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvForgotPassword.setOnClickListener {
            // Handle forgot password
            Toast.makeText(this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.tvSignUp.setOnClickListener {
            // Handle sign up
            Toast.makeText(this, "Sign up feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validate inputs
        if (!ValidationUtils.isValidEmail(email)) {
            binding.etEmail.error = "Invalid email address"
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return
        }

        // Show loading
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnLogin.isEnabled = false

        // Perform login
        lifecycleScope.launch {
            try {
                val deviceInfo = DeviceUtils.getDeviceInfo(this@LoginActivity)
                val loginRequest = com.altarfunds.mobile.models.LoginRequest(
                    email = email,
                    password = password,
                    device_token = deviceInfo.deviceToken,
                    device_type = "android",
                    device_id = deviceInfo.deviceId,
                    app_version = deviceInfo.appVersion,
                    os_version = deviceInfo.osVersion
                )

                val response = ApiService.getApiInterface().login(loginRequest)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    
                    // Save user session
                    val preferencesManager = (application as AltarFundsApp).preferencesManager
                    preferencesManager.saveUserSession(loginResponse)

                    // Navigate to main activity
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val preferencesManager = (application as AltarFundsApp).preferencesManager
        return preferencesManager.isLoggedIn()
    }
}
