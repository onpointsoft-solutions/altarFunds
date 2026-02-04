package com.altarfunds.mobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.altarfunds.mobile.databinding.ActivityMainBinding
import com.altarfunds.mobile.ui.fragments.DashboardFragment
import com.altarfunds.mobile.ui.fragments.GivingFragmentModern
import com.altarfunds.mobile.ui.fragments.ProfileSettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Setup app bar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "AltarFunds"

        // Initialize fragments
        val dashboardFragment = DashboardFragment()
        val givingFragment = GivingFragmentModern()
        val profileFragment = ProfileSettingsFragment()

        // Set default fragment (Dashboard)
        makeCurrentFragment(dashboardFragment)

        // Setup bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    makeCurrentFragment(dashboardFragment)
                    supportActionBar?.title = "Dashboard"
                    true
                }
                R.id.nav_giving -> {
                    makeCurrentFragment(givingFragment)
                    supportActionBar?.title = "Giving"
                    true
                }
                R.id.nav_profile -> {
                    makeCurrentFragment(profileFragment)
                    supportActionBar?.title = "Profile"
                    true
                }
                else -> false
            }
        }
        
        // Ensure bottom navigation is visible
        binding.bottomNavigation.visibility = android.view.View.VISIBLE
    }

    private fun makeCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fl_wrapper, fragment)
            commit()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val preferencesManager = (application as AltarFundsApp).preferencesManager
        return preferencesManager.isLoggedIn()
    }

    fun floatingActionButton(view: android.view.View) {
        startActivity(Intent(this, NewGivingActivity::class.java))
    }
}
