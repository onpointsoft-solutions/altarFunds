package com.altarfunds.member.ui.giving

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.member.databinding.ActivityDonationDetailsBinding

class DonationDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDonationDetailsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Donation Details"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
