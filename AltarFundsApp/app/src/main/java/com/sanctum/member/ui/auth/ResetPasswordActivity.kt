package com.sanctum.member.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import com.sanctum.member.databinding.ActivityResetPasswordBinding
import com.sanctum.member.models.PasswordResetConfirmRequest
import com.sanctum.member.utils.*
import kotlinx.coroutines.launch
import java.util.UUID

class ResetPasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityResetPasswordBinding
    private val app by lazy { MemberApp.getInstance() }
    private var resetToken: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get token from intent
        resetToken = intent.getStringExtra("reset_token")
        
        setupToolbar()
        setupListeners()
        setupPasswordVisibility()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.reset_password)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupListeners() {
        binding.btnResetPassword.setOnClickListener {
            if (validateInput()) {
                resetPassword()
            }
        }
        
        // Handle deep links with token
        handleIntent()
    }
    
    private fun handleIntent() {
        val intent = intent
        val data = intent.data
        
        if (data != null && data.toString().contains("reset-password")) {
            // Extract token from deep link
            val pathSegments = data.pathSegments
            val tokenIndex = pathSegments.indexOf("reset-password")
            if (tokenIndex != -1 && tokenIndex + 1 < pathSegments.size) {
                resetToken = pathSegments[tokenIndex + 1]
                showToast("Reset token loaded from email link")
            }
        }
    }
    
    private fun setupPasswordVisibility() {
        // Password visibility toggle
        binding.ivTogglePassword.setOnClickListener {
            val isVisible = binding.etNewPassword.transformationMethod == null
            binding.etNewPassword.transformationMethod = if (isVisible) {
                PasswordTransformationMethod.getInstance()
            } else {
                null
            }
            binding.ivTogglePassword.setImageResource(
                if (isVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
        }
        
        binding.ivToggleConfirmPassword.setOnClickListener {
            val isVisible = binding.etConfirmPassword.transformationMethod == null
            binding.etConfirmPassword.transformationMethod = if (isVisible) {
                PasswordTransformationMethod.getInstance()
            } else {
                null
            }
            binding.ivToggleConfirmPassword.setImageResource(
                if (isVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )
        }
    }
    
    private fun validateInput(): Boolean {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        // Validate token
        if (resetToken.isNullOrBlank()) {
            showToast("Invalid reset token")
            return false
        }
        
        // Validate new password
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "New password is required"
            return false
        }
        
        if (newPassword.length < 12) {
            binding.tilNewPassword.error = "Password must be at least 12 characters"
            return false
        }
        
        if (!newPassword.isValidPassword()) {
            binding.tilNewPassword.error = "Password must contain uppercase, lowercase, number, and special character"
            return false
        }
        
        // Validate password confirmation
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            return false
        }
        
        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return false
        }
        
        // Clear errors
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null
        
        return true
    }
    
    private fun resetPassword() {
        showLoading(true)
        
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        lifecycleScope.launch {
            try {
                val request = PasswordResetConfirmRequest(
                    token = resetToken ?: "",
                    newPassword = newPassword,
                    newPasswordConfirm = confirmPassword
                )
                
                val response = app.apiService.confirmPasswordReset(request)
                
                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Password reset successful"
                    showToast(message)
                    
                    // Navigate to login screen
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = when {
                        errorBody?.contains("Invalid token") == true -> "Invalid or expired reset token"
                        errorBody?.contains("expired") == true -> "Reset token has expired"
                        errorBody?.contains("used") == true -> "Reset token has already been used"
                        else -> "Failed to reset password. Please try again."
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
            binding.btnResetPassword.isEnabled = false
            binding.progressBar.visible()
        } else {
            binding.btnResetPassword.isEnabled = true
            binding.progressBar.gone()
        }
    }
}
