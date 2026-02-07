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
            try {
                val response = app.apiService.getProfile()
                
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    displayProfile(user)
                } else {
                    requireContext().showToast("Failed to load profile")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("Network error")
            }
        }
    }
    
    private fun displayProfile(user: com.altarfunds.member.models.User) {
        binding.tvName.text = "${user.firstName} ${user.lastName}"
        binding.tvEmail.text = user.email
        binding.tvPhone.text = user.phoneNumber
        binding.tvRole.text = user.role
        binding.tvChurch.text = user.churchInfo?.name ?: "No church"
    }
    
    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
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
