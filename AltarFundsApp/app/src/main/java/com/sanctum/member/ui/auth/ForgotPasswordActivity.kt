package com.sanctum.member.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import com.sanctum.member.databinding.ActivityForgotPasswordBinding
import com.sanctum.member.models.ForgotPasswordRequest
import com.sanctum.member.utils.*
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
                    val message = response.body()?.message ?: "Password reset link sent to your email"
                    showSuccessDialog(email)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = when {
                        errorBody?.contains("Email not found") == true -> 
                            "No account found with this email address"
                        errorBody?.contains("rate limit") == true -> 
                            "Too many reset attempts. Please try again later."
                        else -> "Failed to send reset link. Please try again."
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
    
    private fun showSuccessDialog(email: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Link Sent")
            .setMessage("We've sent a password reset link to:\n\n$email\n\n" +
                    "Please check your email and click the link to reset your password. " +
                    "The link will expire in 1 hour.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
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
