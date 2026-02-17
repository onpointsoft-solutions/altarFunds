package com.altarfunds.member.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.altarfunds.member.R
import android.content.res.Resources
import com.altarfunds.member.databinding.ActivityMainBinding
import com.altarfunds.member.ui.announcements.AnnouncementsFragment
import com.altarfunds.member.ui.dashboard.DashboardFragment
import com.altarfunds.member.ui.devotionals.DevotionalsFragment
import com.altarfunds.member.ui.giving.GivingFragment
import com.altarfunds.member.ui.profile.ProfileFragment
import com.altarfunds.member.utils.ThemeManager
import com.altarfunds.member.viewmodel.GivingViewModel
import com.altarfunds.member.viewmodel.Resource

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GivingViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel for theme loading
        viewModel = ViewModelProvider(this)[GivingViewModel::class.java]
        
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
        
        setupBottomNavigation()
        loadChurchTheme()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_giving -> {
                    loadFragment(GivingFragment())
                    true
                }
                R.id.nav_announcements -> {
                    loadFragment(AnnouncementsFragment())
                    true
                }
                R.id.nav_devotionals -> {
                    loadFragment(DevotionalsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun loadChurchTheme() {
        // Load church theme colors
        viewModel.loadThemeColors()
        
        // Observe theme changes
        viewModel.themeColors.observe(this) { result ->
            when (result) {
                is com.altarfunds.member.utils.ThemeManager-> {
                    // Apply theme to the entire app
                    ThemeManager.applyChurchTheme(this, result.getCurrentTheme())
                }
                is Resource.Error -> {
                    // Continue with default theme
                }
                else -> {}
            }
        }
    }
}
