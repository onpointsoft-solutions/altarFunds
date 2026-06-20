package com.altarfunds.member.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.databinding.FragmentProfileBinding
import com.altarfunds.member.ui.auth.LoginActivity
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }

    // Prevent double load on first onResume
    private var initialLoadDone = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        loadProfile()
        initialLoadDone = true
    }

    override fun onResume() {
        super.onResume()
        // Reload only if returning from EditProfile (not the initial show)
        if (initialLoadDone) loadProfile()
    }

    // ── Data loading ──────────────────────────────────────────────────────

    private fun loadProfile() {
        if (_binding == null) return

        lifecycleScope.launch {
            try {
                val response = app.apiService.getProfile()
                if (_binding == null) return@launch

                if (response.isSuccessful && response.body() != null) {
                    displayProfile(response.body()!!)
                } else {
                    val msg = when (response.code()) {
                        401  -> "Session expired. Please log in again."
                        403  -> "Access denied."
                        404  -> "Profile not found."
                        500  -> "Server error. Please try again later."
                        else -> "Failed to load profile."
                    }
                    requireContext().showToast(msg)
                }
            } catch (e: java.net.UnknownHostException) {
                if (_binding != null) requireContext().showToast("✗ No internet connection.")
            } catch (e: Exception) {
                e.printStackTrace()
                if (_binding != null) requireContext().showToast("✗ ${e.message ?: "Network error"}")
            }
        }
    }

    private fun displayProfile(user: com.altarfunds.member.models.User) {
        binding.tvName.text   = "${user.firstName} ${user.lastName}".trim()
        binding.tvEmail.text  = user.email
        binding.tvPhone.text  = user.phoneNumber ?: "—"
        binding.tvRole.text   = user.role
        binding.tvChurch.text = user.churchInfo?.name ?: "No church"
    }

    // ── Listeners ─────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        binding.btnLogout.setOnClickListener { logout() }
    }

    private fun logout() {
        lifecycleScope.launch {
            app.tokenManager.clearTokens()
            requireContext().showToast("✓ Logged out successfully")

            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
