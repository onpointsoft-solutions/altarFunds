package com.altarfunds.member.ui.church

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.databinding.ActivityChurchTransferBinding
import com.altarfunds.member.models.ChurchTransferRequest
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class ChurchTransferActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChurchTransferBinding
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChurchTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Transfer Church"
        }
        
        // Load current church info
        loadCurrentChurchInfo()
    }
    
    private fun loadCurrentChurchInfo() {
        lifecycleScope.launch {
            try {
                val response = ApiUtils.executeWithRefresh { app.apiService.getProfile() }
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    binding.tvCurrentChurch.text = user.churchInfo?.name ?: "Not assigned to any church"
                }
            } catch (e: Exception) {
                binding.tvCurrentChurch.text = "Unable to load current church"
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSearchChurch.setOnClickListener {
            val churchCode = binding.etChurchCode.text.toString().trim()
            if (churchCode.isEmpty()) {
                showToast("Please enter a church code")
                return@setOnClickListener
            }
            
            searchChurch(churchCode)
        }
        
        binding.btnTransfer.setOnClickListener {
            val churchCode = binding.etChurchCode.text.toString().trim()
            val reason = binding.etReason.text.toString().trim()
            
            if (churchCode.isEmpty()) {
                showToast("Please enter a church code")
                return@setOnClickListener
            }
            
            if (binding.tvNewChurchName.text.toString().isEmpty()) {
                showToast("Please search for a church first")
                return@setOnClickListener
            }
            
            performTransfer(churchCode, reason)
        }
    }
    
    private fun searchChurch(churchCode: String) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showToast("✗ No internet connection")
            return
        }
        
        binding.progressBar.visible()
        
        lifecycleScope.launch {
            try {
                // Search for churches with the given code
                val response = ApiUtils.executeWithRefresh { 
                    app.apiService.getChurches(search = churchCode, page = 1) 
                }
                
                binding.progressBar.gone()
                
                if (response.isSuccessful && response.body() != null) {
                    val churches = response.body()!!.results
                    val matchingChurch = churches.find { it.code.equals(churchCode, ignoreCase = true) }
                    
                    if (matchingChurch != null) {
                        binding.tvNewChurchName.text = matchingChurch.name
                        binding.tvNewChurchDetails.text = buildString {
                            append("Code: ${matchingChurch.code}\n")
                            append("Email: ${matchingChurch.email ?: "Not provided"}\n")
                            append("Phone: ${matchingChurch.phoneNumber ?: "Not provided"}\n")
                            append("Address: ${matchingChurch.addressLine1 ?: "Not provided"}")
                            if (!matchingChurch.city.isNullOrEmpty()) {
                                append(", ${matchingChurch.city}")
                            }
                        }
                        binding.btnTransfer.isEnabled = true
                        showToast("✓ Church found")
                    } else {
                        binding.tvNewChurchName.text = ""
                        binding.tvNewChurchDetails.text = "No church found with this code"
                        binding.btnTransfer.isEnabled = false
                        showToast("✗ No church found with this code")
                    }
                } else {
                    binding.tvNewChurchName.text = ""
                    binding.tvNewChurchDetails.text = "Failed to search for church"
                    binding.btnTransfer.isEnabled = false
                    showToast("✗ Failed to search: ${response.message()}")
                }
                
            } catch (e: Exception) {
                binding.progressBar.gone()
                binding.tvNewChurchName.text = ""
                binding.tvNewChurchDetails.text = "Error searching for church"
                binding.btnTransfer.isEnabled = false
                showToast("✗ Error: ${e.message}")
            }
        }
    }
    
    private fun performTransfer(churchCode: String, reason: String) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showToast("✗ No internet connection")
            return
        }
        
        binding.progressBar.visible()
        binding.btnTransfer.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val request = ChurchTransferRequest(churchCode, reason.ifEmpty { null })
                val response = ApiUtils.executeWithRefresh { 
                    app.apiService.transferChurch(request) 
                }
                
                binding.progressBar.gone()
                binding.btnTransfer.isEnabled = true
                
                if (response.isSuccessful && response.body() != null) {
                    showToast("✓ ${response.body()!!.message}")
                    
                    // Clear form and reload current church info
                    binding.etChurchCode.text?.clear()
                    binding.etReason.text?.clear()
                    binding.tvNewChurchName.text = ""
                    binding.tvNewChurchDetails.text = ""
                    binding.btnTransfer.isEnabled = false
                    
                    // Reload current church info
                    loadCurrentChurchInfo()
                    
                    // Cache updated user profile
                    try {
                        val profileResponse = ApiUtils.executeWithRefresh { app.apiService.getProfile() }
                        if (profileResponse.isSuccessful && profileResponse.body() != null) {
                            val user = profileResponse.body()!!
                            app.database.userDao().insertUser(user.toEntity())
                        }
                    } catch (e: Exception) {
                        // Ignore caching errors
                    }
                    
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "✗ Invalid church code or transfer request"
                        401 -> "✗ Session expired. Please login again."
                        403 -> "✗ Transfer not allowed"
                        404 -> "✗ Church not found"
                        409 -> "✗ Transfer request already pending"
                        else -> "✗ Transfer failed: ${response.message()}"
                    }
                    showToast(errorMessage)
                }
                
            } catch (e: Exception) {
                binding.progressBar.gone()
                binding.btnTransfer.isEnabled = true
                showToast("✗ Transfer failed: ${e.message}")
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
