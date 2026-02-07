package com.altarfunds.member.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityEditProfileBinding
import com.altarfunds.member.models.UpdateProfileRequest
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditProfileBinding
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        loadProfile()
        setupListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.edit_profile)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun loadProfile() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val response = app.apiService.getProfile()
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    binding.etFirstName.setText(user.firstName)
                    binding.etLastName.setText(user.lastName)
                    binding.etEmail.setText(user.email)
                    binding.etPhone.setText(user.phoneNumber)
                } else {
                    showToast("Failed to load profile")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(getString(R.string.network_error))
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                updateProfile()
            }
        }
    }
    
    private fun validateInput(): Boolean {
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        
        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "First name is required"
            return false
        }
        
        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Last name is required"
            return false
        }
        
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }
        
        if (!email.isValidEmail()) {
            binding.tilEmail.error = "Invalid email address"
            return false
        }
        
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            return false
        }
        
        if (!phone.isValidPhone()) {
            binding.tilPhone.error = "Invalid phone number"
            return false
        }
        
        binding.tilFirstName.error = null
        binding.tilLastName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        return true
    }
    
    private fun updateProfile() {
        showLoading(true)
        
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString().formatPhoneNumber()
        
        lifecycleScope.launch {
            try {
                val request = UpdateProfileRequest(
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phone
                )
                
                val response = app.apiService.updateProfile(request)
                
                if (response.isSuccessful) {
                    showToast("Profile updated successfully")
                    finish()
                } else {
                    showToast("Failed to update profile")
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
            binding.btnSave.isEnabled = false
            binding.progressBar.visible()
        } else {
            binding.btnSave.isEnabled = true
            binding.progressBar.gone()
        }
    }
}
