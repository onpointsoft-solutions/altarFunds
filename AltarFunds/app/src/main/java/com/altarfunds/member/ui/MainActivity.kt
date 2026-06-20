package com.altarfunds.member.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.altarfunds.member.MemberApp
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityMainBinding
import com.altarfunds.member.ui.announcements.AnnouncementsFragment
import com.altarfunds.member.ui.auth.LoginActivity
import com.altarfunds.member.ui.dashboard.DashboardFragment
import com.altarfunds.member.ui.devotionals.DevotionalsFragment
import com.altarfunds.member.ui.giving.GivingFragment
import com.altarfunds.member.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app by lazy { MemberApp.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Auth guard ─────────────────────────────────────────────────────
        // If the token was cleared (logout or 401 interceptor) redirect immediately
        if (!app.tokenManager.isLoggedIn()) {
            redirectToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            // Handle tapped notification deep-links before defaulting to Dashboard
            val startTab = intent?.getStringExtra("notification_tab")
            val startFragment: Fragment = when (startTab) {
                "announcements" -> AnnouncementsFragment()
                "devotionals"   -> DevotionalsFragment()
                "giving"        -> GivingFragment()
                else            -> DashboardFragment()
            }
            loadFragment(startFragment)

            // Sync bottom nav selection with start fragment
            binding.bottomNav.selectedItemId = when (startTab) {
                "announcements" -> R.id.nav_announcements
                "devotionals"   -> R.id.nav_devotionals
                "giving"        -> R.id.nav_giving
                else            -> R.id.nav_home
            }
        }

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Re-check auth every time the activity comes to foreground
        // (handles the case where the 401 interceptor cleared tokens while in background)
        if (!app.tokenManager.isLoggedIn()) {
            redirectToLogin()
        }
    }

    // Called by the 401 interceptor redirect and by onResume
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home          -> { loadFragment(DashboardFragment());     true }
                R.id.nav_giving        -> { loadFragment(GivingFragment());        true }
                R.id.nav_announcements -> { loadFragment(AnnouncementsFragment()); true }
                R.id.nav_devotionals   -> { loadFragment(DevotionalsFragment());   true }
                R.id.nav_profile       -> { loadFragment(ProfileFragment());       true }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
