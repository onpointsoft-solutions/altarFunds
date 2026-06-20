package com.altarfunds.member.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityLoginBinding
import com.altarfunds.member.models.LoginRequest
import com.altarfunds.member.ui.MainActivity
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { MemberApp.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Persist session: skip login if token is still valid ────────────
        if (app.tokenManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInput(email, password)) login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var valid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.required_field)
            valid = false
        } else if (!email.isValidEmail()) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.required_field)
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        return valid
    }

    private fun login(email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = app.apiService.login(LoginRequest(email, password))

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Persist tokens — this is what keeps the user logged in across restarts
                    app.tokenManager.saveTokens(body.access, body.refresh)

                    // Persist user identity so the UI can show name/email without an extra API call
                    app.tokenManager.saveUserInfo(
                        body.user.id.toString(),
                        body.user.email
                    )

                    showToast(getString(R.string.login_successful))
                    navigateToMain()

                } else {
                    val msg = when (response.code()) {
                        400  -> "✗ Please check your email and password."
                        401  -> "✗ Invalid email or password."
                        403  -> "✗ Account disabled. Please contact support."
                        404  -> "✗ Account not found. Please register first."
                        500  -> "✗ Server error. Please try again later."
                        else -> "✗ Login failed: ${response.message() ?: "Unknown error"}"
                    }
                    showToast(msg)
                }

            } catch (e: java.net.UnknownHostException) {
                showToast("✗ No internet connection.")
            } catch (e: java.net.SocketTimeoutException) {
                showToast("✗ Connection timed out. Please try again.")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ ${e.message ?: getString(R.string.network_error)}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !show
        binding.tilEmail.isEnabled = !show
        binding.tilPassword.isEnabled = !show
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
