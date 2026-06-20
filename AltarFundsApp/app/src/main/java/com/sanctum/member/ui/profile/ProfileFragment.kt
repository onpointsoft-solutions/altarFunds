package com.sanctum.member.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sanctum.member.MemberApp
import com.sanctum.member.data.mappers.toEntity
import com.sanctum.member.data.mappers.toModel
import com.sanctum.member.databinding.FragmentProfileBinding
import com.sanctum.member.ui.auth.LoginActivity
import com.sanctum.member.ui.church.ChurchTransferActivity
import com.sanctum.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadProfile()
        setupListeners()
    }
    
    private fun loadProfile() {
        lifecycleScope.launch {
            // Clear cache first to ensure fresh data
            app.database.userDao().deleteAllUsers()
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                requireContext().showToast("✗ No internet connection")
                return@launch
            }
            
            // Fetch from network
            try {
                val response = app.apiService.getProfile()
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    // Cache user data
                    app.database.userDao().insertUser(user.toEntity())
                    displayProfile(user)
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "✗ Session expired. Please login again."
                        403 -> "✗ Access denied. Please login again."
                        404 -> "✗ Profile not found."
                        500 -> "✗ Server error. Please try again later."
                        else -> "✗ Failed to load profile: ${response.message() ?: "Unknown error"}"
                    }
                    requireContext().showToast(errorMessage)
                }
            } catch (e: java.net.UnknownHostException) {
                e.printStackTrace()
                requireContext().showToast("✗ No internet connection. Please check your network.")
            } catch (e: java.net.SocketTimeoutException) {
                e.printStackTrace()
                requireContext().showToast("✗ Connection timeout. Please try again.")
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("✗ Error loading profile: ${e.message ?: "Network error"}")
            }
        }
    }
    
    private fun displayProfile(user: com.sanctum.member.models.User) {
        binding.tvName.text = "${user.firstName} ${user.lastName}"
        binding.tvEmail.text = user.email
        binding.tvPhone.text = user.phoneNumber
        binding.tvRole.text = user.role
        
        if (user.churchInfo != null) {
            binding.tvChurch.text = user.churchInfo.name
            binding.btnJoinChurch.gone()
            binding.btnTransferChurch.visible()
        } else {
            binding.tvChurch.text = "No church"
            binding.btnJoinChurch.visible()
            binding.btnTransferChurch.gone()
        }
    }
    
    private fun setupListeners() {
        binding.btnJoinChurch.setOnClickListener {
            val intent = Intent(requireContext(), com.sanctum.member.ui.church.ChurchSearchActivity::class.java)
            intent.putExtra("SELECT_MODE", false)
            startActivity(intent)
        }
        
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        
        binding.btnTransferChurch.setOnClickListener {
            startActivity(Intent(requireContext(), ChurchTransferActivity::class.java))
        }
        
        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }
        
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun logout() {
        lifecycleScope.launch {
            try {
                // Deregister FCM token from backend so no notifications
                // are sent to this device after logout
                app.apiService.deregisterFcmToken()
            } catch (e: Exception) {
                // Non-fatal — proceed with local logout regardless
            }

            // Clear auth tokens from DataStore
            app.tokenManager.clearTokens()

            // Clear user/church identity from SharedPreferences so
            // notification relevance checks start clean on next login
            app.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .remove("user_id")
                .remove("church_id")
                .remove("fcm_token")
                .apply()

            requireContext().showToast("✓ Logged out successfully")

            val intent = android.content.Intent(requireContext(), LoginActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                           android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
