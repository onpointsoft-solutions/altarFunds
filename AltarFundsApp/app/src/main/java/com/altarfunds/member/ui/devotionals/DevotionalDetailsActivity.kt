package com.altarfunds.member.ui.devotionals

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.member.databinding.ActivityDevotionalDetailsBinding

class DevotionalDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDevotionalDetailsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevotionalDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Devotional Details"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
