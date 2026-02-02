package com.altarfunds.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.mobile.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadUserProfile()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = com.altarfunds.mobile.api.ApiService.getUserProfile()
                
                withContext(Dispatchers.Main) {
                    binding.etFirstName.setText(profile.first_name)
                    binding.etLastName.setText(profile.last_name)
                    binding.etEmail.setText(profile.email)
                    binding.etPhone.setText(profile.phone_number ?: "")
                    binding.etAddress.setText(profile.address_line1 ?: "")
                    binding.etCity.setText(profile.city ?: "")
                    binding.etCountry.setText(profile.county ?: "")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Failed to load profile: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        binding.btnChangePassword.setOnClickListener {
            // TODO: Navigate to change password activity
            Toast.makeText(this, "Change password feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.ivProfilePicture.setOnClickListener {
            // TODO: Open image picker for profile picture
            Toast.makeText(this, "Profile picture upload coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfile() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val country = binding.etCountry.text.toString().trim()

        // Validate inputs
        if (firstName.isEmpty()) {
            binding.etFirstName.error = "First name is required"
            return
        }
        
        if (lastName.isEmpty()) {
            binding.etLastName.error = "Last name is required"
            return
        }
        
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }

        // Show loading state
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profileUpdate = com.altarfunds.mobile.models.ProfileUpdate(
                    first_name = firstName,
                    last_name = lastName,
                    phone_number = phone.ifEmpty { null }
                )
                
                val response = com.altarfunds.mobile.api.ApiService.getApiInterface().updateProfile(profileUpdate)
                
                withContext(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                    
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                    Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
