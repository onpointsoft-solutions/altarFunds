package com.altarfunds.mobile

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.databinding.ActivityChurchDetailsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChurchDetailsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChurchDetailsActivity"
        const val EXTRA_CHURCH_ID = "church_id"
        const val EXTRA_CHURCH_NAME = "church_name"
    }

    private lateinit var binding: ActivityChurchDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChurchDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadChurchDetails()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(EXTRA_CHURCH_NAME) ?: "Church Details"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadChurchDetails() {
        val churchId = intent.getStringExtra(EXTRA_CHURCH_ID)
        
        if (churchId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // TODO: Implement API call to get church details
                    withContext(Dispatchers.Main) {
                        // For now, show placeholder data
                        showPlaceholderData()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load church details", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ChurchDetailsActivity,
                            "Failed to load church details",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            showPlaceholderData()
        }
    }

    private fun showPlaceholderData() {
        binding.tvChurchName.text = intent.getStringExtra(EXTRA_CHURCH_NAME) ?: "Church Name"
        binding.tvChurchDescription.text = "A wonderful church serving the community with love and faith."
        binding.tvChurchAddress.text = "123 Church Street, City, Country"
        binding.tvChurchPhone.text = "+254 123 456 789"
        binding.tvChurchEmail.text = "info@church.com"
        binding.tvChurchWebsite.text = "www.church.com"
    }

    private fun setupClickListeners() {
        binding.btnCall.setOnClickListener {
            val phone = binding.tvChurchPhone.text.toString()
            // TODO: Implement phone call
            Toast.makeText(this, "Calling $phone", Toast.LENGTH_SHORT).show()
        }

        binding.btnEmail.setOnClickListener {
            val email = binding.tvChurchEmail.text.toString()
            // TODO: Implement email intent
            Toast.makeText(this, "Emailing $email", Toast.LENGTH_SHORT).show()
        }

        binding.btnWebsite.setOnClickListener {
            val website = binding.tvChurchWebsite.text.toString()
            // TODO: Implement website intent
            Toast.makeText(this, "Opening $website", Toast.LENGTH_SHORT).show()
        }

        binding.btnDirections.setOnClickListener {
            val address = binding.tvChurchAddress.text.toString()
            // TODO: Implement maps intent
            Toast.makeText(this, "Getting directions to $address", Toast.LENGTH_SHORT).show()
        }

        binding.btnGive.setOnClickListener {
            // TODO: Navigate to giving activity
            Toast.makeText(this, "Navigate to giving", Toast.LENGTH_SHORT).show()
        }
    }
}
