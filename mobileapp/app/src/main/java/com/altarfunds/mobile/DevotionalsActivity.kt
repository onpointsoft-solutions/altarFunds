package com.altarfunds.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.altarfunds.mobile.databinding.ActivityDevotionalsBinding

class DevotionalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDevotionalsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevotionalsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Devotionals"
    }
}
