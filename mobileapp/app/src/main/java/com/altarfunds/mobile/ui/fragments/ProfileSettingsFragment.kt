package com.altarfunds.mobile.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.altarfunds.mobile.AltarFundsApp
import com.altarfunds.mobile.LoginActivity
import com.altarfunds.mobile.databinding.FragmentProfileSettingsBinding
import com.altarfunds.mobile.data.api.ApiClient
import com.altarfunds.mobile.data.models.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProfileSettingsFragment : Fragment() {

    private var _binding: FragmentProfileSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val apiClient = ApiClient()
    private lateinit var preferencesManager: com.altarfunds.mobile.utils.PreferencesManager
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSettingsBinding.inflate(inflater, container, false)
        preferencesManager = (requireActivity().application as AltarFundsApp).preferencesManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadUserProfile()
        setupClickListeners()
    }

    private fun loadUserProfile() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                currentUser = apiClient.getCurrentUser()
                displayUserInfo()
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showError("Failed to load profile: ${e.message}")
            }
        }
    }

    private fun displayUserInfo() {
        currentUser?.let { user ->
            // Profile Information
            binding.tvUserName.text = "${user.firstName} ${user.lastName}"
            binding.tvUserEmail.text = user.email
            binding.tvUserRole.text = getRoleDisplayName(user.role)
            binding.tvChurchName.text = user.churchName ?: "No church assigned"
            
            // Set initials for avatar
            val initials = "${user.firstName.firstOrNull() ?: ""}${user.lastName.firstOrNull() ?: ""}"
            binding.tvUserInitials.text = initials.uppercase()
            
            // Verification badges
            binding.ivEmailVerified.visibility = if (user.isEmailVerified) View.VISIBLE else View.GONE
            binding.ivPhoneVerified.visibility = if (user.isPhoneVerified) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        // Edit Profile
        binding.cardEditProfile.setOnClickListener {
            // Navigate to edit profile screen
            // TODO: Implement edit profile
            showComingSoon("Edit Profile")
        }

        // Change Password
        binding.cardChangePassword.setOnClickListener {
            // Navigate to change password screen
            // TODO: Implement change password
            showComingSoon("Change Password")
        }

        // Notification Settings
        binding.cardNotifications.setOnClickListener {
            // Navigate to notification settings
            // TODO: Implement notification settings
            showComingSoon("Notification Settings")
        }

        // Privacy & Security
        binding.cardPrivacy.setOnClickListener {
            // Navigate to privacy settings
            // TODO: Implement privacy settings
            showComingSoon("Privacy & Security")
        }

        // Help & Support
        binding.cardHelp.setOnClickListener {
            // Navigate to help screen
            // TODO: Implement help screen
            showComingSoon("Help & Support")
        }

        // About
        binding.cardAbout.setOnClickListener {
            // Show about dialog
            showAboutDialog()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun getRoleDisplayName(role: String): String {
        return when (role.lowercase()) {
            "admin" -> "Church Administrator"
            "pastor" -> "Pastor"
            "treasurer" -> "Treasurer"
            "member" -> "Church Member"
            "auditor" -> "Auditor"
            else -> role.replaceFirstChar { it.uppercase() }
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                // Call logout API
                apiClient.logout()
                
                // Clear local preferences
                preferencesManager.clearUserData()
                
                // Navigate to login screen
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            } catch (e: Exception) {
                showError("Logout failed: ${e.message}")
            }
        }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About AltarFunds")
            .setMessage("""
                AltarFunds Mobile
                Version 1.0.0
                
                A comprehensive church management system for tracking donations, expenses, and member contributions.
                
                © 2026 AltarFunds. All rights reserved.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showComingSoon(feature: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Coming Soon")
            .setMessage("$feature feature will be available in the next update.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showError(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
