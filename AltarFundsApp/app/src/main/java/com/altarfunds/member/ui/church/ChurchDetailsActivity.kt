package com.altarfunds.member.ui.church

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.data.mappers.toModel
import com.altarfunds.member.databinding.ActivityChurchDetailsBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ChurchDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChurchDetailsBinding
    private val app by lazy { MemberApp.getInstance() }
    private var churchId: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChurchDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        churchId = intent.getIntExtra("id", 0)
        
        setupToolbar()
        loadChurchDetails()
        setupListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Church Details"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun loadChurchDetails() {
        binding.progressBar.visible()
        
        lifecycleScope.launch {
            // First, try to load from cache
            val cachedChurch = app.database.churchDao().getChurch(churchId).firstOrNull()
            if (cachedChurch != null) {
                displayChurchDetails(cachedChurch.toModel())
            }
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(this@ChurchDetailsActivity)) {
                binding.progressBar.gone()
                if (cachedChurch != null) {
                    showToast("ℹ Offline mode - Showing cached church details")
                } else {
                    showToast("✗ No internet connection and church not cached")
                    finish()
                }
                return@launch
            }
            
            // Fetch from network
            try {
                val response = app.apiService.getChurchDetails(churchId)
                
                if (response.isSuccessful && response.body() != null) {
                    val church = response.body()!!
                    // Cache the church details
                    app.database.churchDao().insertChurch(church.toEntity())
                    displayChurchDetails(church)
                } else {
                    if (cachedChurch == null) {
                        val errorMessage = when (response.code()) {
                            404 -> "✗ Church not found. It may have been removed."
                            403 -> "✗ You don't have permission to view this church."
                            500 -> "✗ Server error. Please try again later."
                            else -> "✗ Failed to load church details: ${response.message()}"
                        }
                        showToast(errorMessage)
                        finish()
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                e.printStackTrace()
                if (cachedChurch == null) {
                    showToast("✗ No internet connection. Please check your network.")
                    finish()
                }
            } catch (e: java.net.SocketTimeoutException) {
                e.printStackTrace()
                if (cachedChurch == null) {
                    showToast("✗ Connection timeout. Please try again.")
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cachedChurch == null) {
                    showToast("✗ Error loading church: ${e.message ?: "Network error"}")
                    finish()
                }
            } finally {
                binding.progressBar.gone()
            }
        }
    }
    
    private fun displayChurchDetails(church: com.altarfunds.member.models.Church) {
        binding.tvName.text = church.name
        binding.tvCode.text = church.code
        binding.tvDenomination.text = church.denominationName ?: "N/A"
        binding.tvDenominationLabel.text = church.name
        binding.tvLocation.text = church.city ?: "N/A"
        binding.tvEmail.text = church.email ?: "N/A"
        binding.tvPhone.text = church.phoneNumber ?: "N/A"
        binding.tvDescription.text = church.description ?: "No description available"
    }
    
    private fun setupListeners() {
        binding.btnJoin.setOnClickListener {
            joinChurch()
        }
    }
    
    private fun joinChurch() {
        binding.btnJoin.isEnabled = false
        binding.progressBar.visible()
        
        lifecycleScope.launch {
            try {
                val response = app.apiService.joinChurch(churchId)
                
                if (response.isSuccessful) {
                    showToast("✓ Successfully joined church! Welcome to the community.")
                    finish()
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "✗ Invalid request. You may already be a member."
                        403 -> "✗ You don't have permission to join this church."
                        404 -> "✗ Church not found."
                        409 -> "✗ You are already a member of this church."
                        500 -> "✗ Server error. Please try again later."
                        else -> "✗ Failed to join church: ${response.message() ?: "Unknown error"}"
                    }
                    showToast(errorMessage)
                    binding.btnJoin.isEnabled = true
                }
            } catch (e: java.net.UnknownHostException) {
                e.printStackTrace()
                showToast("✗ No internet connection. Please check your network.")
                binding.btnJoin.isEnabled = true
            } catch (e: java.net.SocketTimeoutException) {
                e.printStackTrace()
                showToast("✗ Connection timeout. Please try again.")
                binding.btnJoin.isEnabled = true
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ Error joining church: ${e.message ?: "Network error"}")
                binding.btnJoin.isEnabled = true
            } finally {
                binding.progressBar.gone()
            }
        }
    }
}
