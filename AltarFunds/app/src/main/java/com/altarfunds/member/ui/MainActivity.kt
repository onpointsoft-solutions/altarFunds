package com.altarfunds.member.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityMainBinding
import com.altarfunds.member.ui.announcements.AnnouncementsFragment
import com.altarfunds.member.ui.dashboard.DashboardFragment
import com.altarfunds.member.ui.devotionals.DevotionalsFragment
import com.altarfunds.member.ui.giving.GivingFragment
import com.altarfunds.member.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
        
        setupBottomNavigation()
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
}
