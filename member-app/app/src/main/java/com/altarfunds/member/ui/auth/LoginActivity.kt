package com.altarfunds.member.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityLoginBinding
import com.altarfunds.member.models.LoginRequest
import com.altarfunds.member.ui.MainActivity
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if already logged in
        if (app.tokenManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        setupListeners()
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (validateInput(email, password)) {
                login(email, password)
            }
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.required_field)
            return false
        }
        
        if (!email.isValidEmail()) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            return false
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.required_field)
            return false
        }
        
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        return true
    }
    
    private fun login(email: String, password: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val request = LoginRequest(email, password)
                val response = app.apiService.login(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    
                    // Save tokens
                    app.tokenManager.saveTokens(
                        loginResponse.access,
                        loginResponse.refresh
                    )
                    
                    // Save user info
                    app.tokenManager.saveUserInfo(
                        loginResponse.user.id.toString(),
                        loginResponse.user.email
                    )
                    
                    showToast(getString(R.string.login_successful))
                    navigateToMain()
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Invalid email or password"
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
            binding.btnLogin.isEnabled = false
        } else {
            binding.progressBar.gone()
            binding.btnLogin.isEnabled = true
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
