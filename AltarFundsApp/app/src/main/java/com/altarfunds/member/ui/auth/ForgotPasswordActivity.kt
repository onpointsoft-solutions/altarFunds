package com.altarfunds.member.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityForgotPasswordBinding
import com.altarfunds.member.models.ForgotPasswordRequest
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityForgotPasswordBinding
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.forgot_password)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupListeners() {
        binding.btnResetPassword.setOnClickListener {
            if (validateInput()) {
                resetPassword()
            }
        }
    }
    
    private fun validateInput(): Boolean {
        val email = binding.etEmail.text.toString()
        
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }
        
        if (!email.isValidEmail()) {
            binding.tilEmail.error = "Invalid email address"
            return false
        }
        
        binding.tilEmail.error = null
        return true
    }
    
    private fun resetPassword() {
        showLoading(true)
        
        val email = binding.etEmail.text.toString()
        
        lifecycleScope.launch {
            try {
                val request = ForgotPasswordRequest(email = email)
                val response = app.apiService.forgotPassword(request)
                
                if (response.isSuccessful) {
                    showToast("Password reset link sent to your email")
                    finish()
                } else {
                    showToast("Failed to send reset link")
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
            binding.btnResetPassword.isEnabled = false
            binding.progressBar.visible()
        } else {
            binding.btnResetPassword.isEnabled = true
            binding.progressBar.gone()
        }
    }
}
