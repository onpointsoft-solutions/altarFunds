package com.altarfunds.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.mobile.databinding.ActivityForgotPasswordBinding
import com.altarfunds.mobile.utils.ValidationUtils.isValidEmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        // No toolbar in this layout, just set title in action bar
        supportActionBar?.title = "Reset Password"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupClickListeners() {
        binding.forgotPassBtn.setOnClickListener {
            validateAndResetPassword()
        }
    }

    private fun validateAndResetPassword() {
        val email = binding.emailForgotPass.text.toString().trim()
        
        if (email.isEmpty()) {
            binding.emailForgotPass.error = "Email is required"
            return
        }
        
        if (!isValidEmail(email)) {
            binding.emailForgotPass.error = "Please enter a valid email"
            return
        }
        
        resetPassword(email)
    }

    private fun resetPassword(email: String) {
        // Show loading state
        binding.forgotPassBtn.isEnabled = false
        binding.forgotPassBtn.text = "Sending..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // TODO: Implement API call to reset password
                kotlinx.coroutines.delay(2000) // Simulate API call
                
                withContext(Dispatchers.Main) {
                    binding.forgotPassBtn.isEnabled = true
                    binding.forgotPassBtn.text = "Submit"
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Password reset link sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.forgotPassBtn.isEnabled = true
                    binding.forgotPassBtn.text = "Submit"
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Failed to send reset link. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
