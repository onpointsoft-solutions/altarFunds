package com.sanctum.member.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import com.sanctum.member.data.mappers.toEntity
import com.sanctum.member.databinding.ActivityLoginBinding
import com.sanctum.member.models.LoginRequest
import com.sanctum.member.ui.MainActivity
import com.sanctum.member.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { MemberApp.getInstance() }
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
        progressDialog = ProgressDialog(this)
        setupInputValidation()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            if (validateInput()) login()
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
            binding.etEmail    to binding.tilEmail,
            binding.etPassword to binding.tilPassword,
        )
    }

    private fun setupInputValidation() {
        InputValidator.addEmailValidation(binding.etEmail, binding.tilEmail)
        InputValidator.addRequiredFieldValidation(binding.etPassword, binding.tilPassword, "Password")
    }

    // ── Login flow ────────────────────────────────────────────────────────

    private fun login() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        progressDialog.setMessage("Signing in…")
        progressDialog.show()

        lifecycleScope.launch {
            try {
                // ── 1. Authenticate with Django backend ───────────────────
                val response = app.apiService.login(LoginRequest(email, password))

                if (!response.isSuccessful || response.body() == null) {
                    progressDialog.dismiss()
                    showToast(httpErrorMessage(response.code(), response.message()))
                    return@launch
                }

                val body = response.body()!!

                // ── 2. Persist Django JWT tokens ──────────────────────────
                app.tokenManager.saveTokens(body.access, body.refresh)

                // ── 3. Cache user / church identity ───────────────────────
                val user = body.user ?: fetchProfile() ?: run {
                    progressDialog.dismiss()
                    showToast("Login succeeded but profile load failed. Please try again.")
                    return@launch
                }

                app.tokenManager.saveUserInfo(
                    user.id.toString(),
                    user.email,
                    user.churchInfo?.id?.toString() ?: user.church?.toString(),
                )
                app.saveUserInfo(
                    user.id.toString(),
                    user.churchInfo?.id?.toString() ?: user.church?.toString(),
                )
                app.database.userDao().insertUser(user.toEntity())

                // ── 4. Register FCM token + schedule sync ─────────────────
                app.onUserLoggedIn()

                // ── 5. Sign into Firebase Auth ────────────────────────────
                // The Django backend creates / mirrors the Firebase Auth account
                // asynchronously (Celery task). If the task hasn't run yet on
                // first login, Firebase Auth will return INVALID_CREDENTIALS.
                // We treat this as non-fatal — the app works without a Firebase
                // session, and the account will exist on the next login attempt.
                signIntoFirebase(email, password)

                progressDialog.dismiss()
                showToast("✓ Welcome back!")
                binding.btnLogin.animateSuccess { navigateToMain() }

            } catch (e: java.net.UnknownHostException) {
                progressDialog.dismiss()
                showToast("✗ No internet connection.")
            } catch (e: java.net.SocketTimeoutException) {
                progressDialog.dismiss()
                showToast("✗ Connection timed out. Please try again.")
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e(TAG, "Login exception", e)
                showToast("✗ ${e.message ?: getString(R.string.network_error)}")
            }
        }
    }

    /**
     * Fetch profile from the API as a fallback when the login response
     * doesn't include the user object.
     */
    private suspend fun fetchProfile() = try {
        val r = app.apiService.getProfile()
        if (r.isSuccessful) r.body() else null
    } catch (e: Exception) {
        Log.e(TAG, "Profile fetch failed", e)
        null
    }

    /**
     * Sign into Firebase Auth with email + password.
     * Non-fatal: logs the outcome and returns, never throws.
     *
     * Uses [suspendCancellableCoroutine] to bridge the Firebase Task callback
     * into a Kotlin coroutine so we stay on the lifecycleScope.
     */
    private suspend fun signIntoFirebase(email: String, password: String) {
        suspendCancellableCoroutine<Unit> { cont ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Firebase Auth sign-in successful")
                    } else {
                        // Non-fatal: backend Celery task may not have run yet
                        Log.w(TAG, "Firebase Auth sign-in failed (non-fatal): ${task.exception?.message}")
                    }
                    if (cont.isActive) cont.resume(Unit)
                }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun httpErrorMessage(code: Int, fallback: String?): String = when (code) {
        400  -> "✗ Please check your email and password."
        401  -> "✗ Invalid email or password."
        403  -> "✗ Account disabled. Contact support."
        404  -> "✗ Account not found. Please register."
        500  -> "✗ Server error. Please try again later."
        else -> "✗ Login failed: ${fallback ?: "Unknown error"}"
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
