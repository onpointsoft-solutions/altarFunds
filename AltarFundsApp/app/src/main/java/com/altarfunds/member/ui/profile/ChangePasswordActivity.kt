package com.altarfunds.member.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityChangePasswordBinding
import com.altarfunds.member.models.ChangePasswordRequest
import com.altarfunds.member.utils.*
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
        
        if (newPassword.length < 8) {
            binding.tilNewPassword.error = "Password must be at least 8 characters"
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
        
        val oldPassword = binding.etOldPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        
        lifecycleScope.launch {
            try {
                val request = ChangePasswordRequest(
                    oldPassword = oldPassword,
                    newPassword = newPassword
                )
                
                val response = app.apiService.changePassword(request)
                
                if (response.isSuccessful) {
                    showToast("Password changed successfully")
                    finish()
                } else {
                    showToast("Failed to change password")
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
            binding.btnChangePassword.isEnabled = false
            binding.progressBar.visible()
        } else {
            binding.btnChangePassword.isEnabled = true
            binding.progressBar.gone()
        }
    }
}
