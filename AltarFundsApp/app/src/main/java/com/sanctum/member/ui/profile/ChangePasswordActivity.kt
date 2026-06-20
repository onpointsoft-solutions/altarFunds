package com.sanctum.member.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import com.sanctum.member.databinding.ActivityChangePasswordBinding
import com.sanctum.member.models.ChangePasswordRequest
import com.sanctum.member.utils.*
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChangePasswordBinding
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.change_password)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupListeners() {
        binding.btnChangePassword.setOnClickListener {
            if (validateInput()) {
                changePassword()
            }
        }
    }
    
    private fun validateInput(): Boolean {
        val oldPassword = binding.etOldPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
        if (oldPassword.isEmpty()) {
            binding.tilOldPassword.error = "Current password is required"
            return false
        }
        
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "New password is required"
            return false
        }
        
        if (newPassword.length < 12) {
            binding.tilNewPassword.error = "Password must be at least 12 characters"
            return false
        }
        
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Confirm password is required"
            return false
        }
        
        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return false
        }
        
        binding.tilOldPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null
        return true
    }
    
    private fun changePassword() {
        showLoading(true)

        val currentPassword = binding.etOldPassword.text.toString()
        val newPassword     = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        lifecycleScope.launch {
            try {
                val request = ChangePasswordRequest(
                    currentPassword    = currentPassword,
                    newPassword        = newPassword,
                    newPasswordConfirm = confirmPassword
                )

                val response = app.apiService.changePassword(request)

                if (response.isSuccessful) {
                    showToast("✓ Password changed successfully")
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val msg = when {
                        errorBody?.contains("current_password", ignoreCase = true) == true ->
                            "✗ Current password is incorrect"
                        errorBody?.contains("new_password", ignoreCase = true) == true ->
                            "✗ New password does not meet requirements"
                        response.code() == 400 -> "✗ Invalid request. Please check your inputs."
                        response.code() == 401 -> "✗ Session expired. Please log in again."
                        else -> "✗ Failed to change password (${response.code()})"
                    }
                    showToast(msg)
                }
            } catch (e: java.net.UnknownHostException) {
                showToast("✗ No internet connection")
            } catch (e: java.net.SocketTimeoutException) {
                showToast("✗ Connection timed out. Please try again.")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ ${e.message ?: "Network error"}")
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            binding.btnChangePassword.isEnabled = false
            binding.progressBar.visible()
        } else {
            binding.btnChangePassword.isEnabled = true
            binding.progressBar.gone()
        }
    }
}
