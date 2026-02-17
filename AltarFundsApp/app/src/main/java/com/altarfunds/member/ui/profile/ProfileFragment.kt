package com.altarfunds.member.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.data.mappers.toModel
import com.altarfunds.member.databinding.FragmentProfileBinding
import com.altarfunds.member.ui.auth.LoginActivity
import com.altarfunds.member.ui.church.ChurchTransferActivity
import com.altarfunds.member.utils.*
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
            // First, try to load from cache
            val cachedUser = app.database.userDao().getCurrentUser().firstOrNull()
            if (cachedUser != null) {
                displayProfile(cachedUser.toModel())
            }
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                if (cachedUser != null) {
                    requireContext().showToast("ℹ Offline mode - Showing cached data")
                } else {
                    requireContext().showToast("✗ No internet connection and no cached data available")
                }
                return@launch
            }
            
            // Fetch from network
            try {
                val response = app.apiService.getProfile()
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    // Cache the user data
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
                if (cachedUser == null) {
                    requireContext().showToast("✗ No internet connection. Please check your network.")
                }
            } catch (e: java.net.SocketTimeoutException) {
                e.printStackTrace()
                if (cachedUser == null) {
                    requireContext().showToast("✗ Connection timeout. Please try again.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cachedUser == null) {
                    requireContext().showToast("✗ Error loading profile: ${e.message ?: "Network error"}")
                }
            }
        }
    }
    
    private fun displayProfile(user: com.altarfunds.member.models.User) {
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
            val intent = Intent(requireContext(), com.altarfunds.member.ui.church.ChurchSearchActivity::class.java)
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
            app.tokenManager.clearTokens()
            requireContext().showToast("✓ Logged out successfully")
            
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadProfile()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
