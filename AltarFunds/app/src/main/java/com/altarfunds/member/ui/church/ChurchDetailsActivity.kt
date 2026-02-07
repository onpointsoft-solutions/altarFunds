package com.altarfunds.member.ui.church

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityChurchDetailsBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class ChurchDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChurchDetailsBinding
    private val app by lazy { MemberApp.getInstance() }
    private var churchId: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChurchDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        churchId = intent.getIntExtra("CHURCH_ID", 0)
        
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
            try {
                val response = app.apiService.getChurchDetails(churchId)
                
                if (response.isSuccessful && response.body() != null) {
                    val church = response.body()!!
                    displayChurchDetails(church)
                } else {
                    showToast("Failed to load church details")
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(getString(R.string.network_error))
                finish()
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
                    showToast("Successfully joined church")
                    finish()
                } else {
                    showToast("Failed to join church")
                    binding.btnJoin.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(getString(R.string.network_error))
                binding.btnJoin.isEnabled = true
            } finally {
                binding.progressBar.gone()
            }
        }
    }
}
