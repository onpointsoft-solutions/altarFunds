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
        // TODO: Load user profile from API or local storage
        // For now, set placeholder data
        binding.etFirstName.setText("John")
        binding.etLastName.setText("Doe")
        binding.etEmail.setText("john.doe@example.com")
        binding.etPhone.setText("+254712345678")
        binding.etAddress.setText("123 Main Street")
        binding.etCity.setText("Nairobi")
        binding.etCountry.setText("Kenya")
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
                // TODO: Implement API call to save profile
                kotlinx.coroutines.delay(2000) // Simulate API call
                
                withContext(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                    Toast.makeText(this@EditProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
