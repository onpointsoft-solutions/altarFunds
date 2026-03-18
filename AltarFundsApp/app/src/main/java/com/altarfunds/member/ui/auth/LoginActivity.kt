package com.altarfunds.member.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.databinding.ActivityLoginBinding
import com.altarfunds.member.models.LoginRequest
import com.altarfunds.member.ui.MainActivity
import com.altarfunds.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var progressDialog: ProgressDialog
    
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
        progressDialog = ProgressDialog(this)
        setupInputValidation()
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            if (validateInput()) {
                login()
            }
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
    
    private fun validateInput(): Boolean {
        return InputValidator.validateForm(
            binding.etEmail to binding.tilEmail,
            binding.etPassword to binding.tilPassword
        )
    }
    
    private fun setupInputValidation() {
        InputValidator.addEmailValidation(binding.etEmail, binding.tilEmail)
        InputValidator.addRequiredFieldValidation(binding.etPassword, binding.tilPassword, "Password")
    }
    
    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        progressDialog.setMessage("Signing in...")
        progressDialog.show()
        
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
                    
                    // Fetch user profile if not included in login response
                    if (loginResponse.user != null) {
                        app.tokenManager.saveUserInfo(
                            loginResponse.user.id.toString(),
                            loginResponse.user.email
                        )
                        // Cache user data
                        app.database.userDao().insertUser(loginResponse.user.toEntity())
                    } else {
                        // Fetch profile from API
                        try {
                            val profileResponse = app.apiService.getProfile()
                            if (profileResponse.isSuccessful && profileResponse.body() != null) {
                                val user = profileResponse.body()!!
                                app.tokenManager.saveUserInfo(
                                    user.id.toString(),
                                    user.email
                                )
                                // Cache user data
                                app.database.userDao().insertUser(user.toEntity())
                            } else {
                                showToast("Login successful, but failed to load profile. Please try again.")
                                return@launch
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showToast("Login successful, but network error loading profile. Please restart the app.")
                            return@launch
                        }
                    }
                    
                    showToast("✓ Welcome back! Login successful")
                    binding.btnLogin.animateSuccess {
                        navigateToMain()
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "✗ Invalid email or password. Please check your credentials."
                        400 -> "✗ Invalid login request. Please check your email and password."
                        403 -> "✗ Your account has been disabled. Please contact support."
                        404 -> "✗ Account not found. Please register first."
                        500 -> "✗ Server error. Please try again later."
                        else -> "✗ Login failed: ${response.message() ?: "Unknown error"}"
                    }
                    showToast(errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                e.printStackTrace()
                showToast("✗ No internet connection. Please check your network.")
            } catch (e: java.net.SocketTimeoutException) {
                e.printStackTrace()
                showToast("✗ Connection timeout. Please check your internet and try again.")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ Network error: ${e.message ?: "Unable to connect to server"}")
            } finally {
                progressDialog.dismiss()
            }
        }
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
