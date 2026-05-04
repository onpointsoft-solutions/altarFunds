package com.sanctum.member.ui.announcements

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sanctum.member.databinding.ActivityAnnouncementDetailsBinding

class AnnouncementDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAnnouncementDetailsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnnouncementDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Announcement Details"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
