package com.sanctum.member.ui

import android.os.Bundle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import android.os.PowerManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.res.Resources
import com.sanctum.member.databinding.ActivityMainBinding
import com.sanctum.member.ui.announcements.AnnouncementsFragment
import com.sanctum.member.ui.auth.LoginActivity
import com.sanctum.member.ui.dashboard.DashboardFragment
import com.sanctum.member.ui.devotionals.DevotionalDetailsActivity
import com.sanctum.member.ui.devotionals.DevotionalsFragment
import com.sanctum.member.ui.giving.GivingFragment
import com.sanctum.member.ui.profile.ProfileFragment
import com.sanctum.member.utils.ThemeManager
import com.sanctum.member.utils.showToast
import com.sanctum.member.viewmodel.GivingViewModel
import com.sanctum.member.viewmodel.Resource
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: GivingViewModel
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check notification permissions
        checkNotificationPermission()
        
        // Check if user is still authenticated
        if (!app.tokenManager.isLoggedIn.value) {
            navigateToLogin()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel for theme loading
        viewModel = ViewModelProvider(this)[GivingViewModel::class.java]
        
        // Handle notification intents
        handleNotificationIntent(intent)
        
        // Handle deep links
        handleDeepLink(intent)
        
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
        
        setupBottomNavigation()
        loadChurchTheme()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                
                // Check if we should show rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationPermissionRationale()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            } else {
                // Permission already granted, initialize background work
                initializeBackgroundWork()
            }
        } else {
            // Pre-Android 13, notifications work by default
            initializeBackgroundWork()
        }
    }
    
    private fun initializeBackgroundWork() {
        // Initialize WorkManager for background processing
        try {
            val workManager = WorkManager.getInstance(this)
            
            // Schedule periodic notification sync
            schedulePeriodicNotificationSync(workManager)
            
            // Request battery optimization whitelist
            requestBatteryOptimizationWhitelist()
            
            Log.d("MainActivity", "Background work initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize background work", e)
        }
    }
    
    private fun schedulePeriodicNotificationSync(workManager: WorkManager) {
        try {
            // Create constraints for periodic work
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            // Create periodic work request (every 6 hours)
            val periodicSyncRequest = PeriodicWorkRequestBuilder<com.sanctum.member.notification.NotificationSyncWorker>(
                6, // repeat interval (hours)
                java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            // Enqueue the periodic work
            workManager.enqueueUniquePeriodicWork(
                "notification_sync",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                periodicSyncRequest
            )
            
            Log.d("MainActivity", "Periodic notification sync scheduled (every 6 hours)")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to schedule periodic sync", e)
        }
    }
    
    private fun requestBatteryOptimizationWhitelist() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val packageName = packageName
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d("MainActivity", "Requesting battery optimization whitelist")
                
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to launch battery optimization settings", e)
                }
            } else {
                Log.d("MainActivity", "App is already whitelisted from battery optimization")
            }
        }
    }
    
    private fun handleNotificationIntent(intent: Intent?) {
        val type    = intent?.getStringExtra("notification_type")
        val title   = intent?.getStringExtra("title")
        val message = intent?.getStringExtra("message")

        // If no notification data, this is a normal app launch — do nothing
        if (type == null && title == null && message == null) return

        when (type) {
            "devotional_new", "devotional_shared" -> {
                navigateToDevotional(intent?.getStringExtra("devotional_id"))
            }
            "announcement", "announcement_posted" -> {
                navigateToAnnouncements()
            }
            else -> {
                // General notifications show a dialog, but only if they have actual content
                if (!title.isNullOrBlank() || !message.isNullOrBlank()) {
                    showNotificationDialog(title, message)
                }
            }
        }
    }

    private fun navigateToDevotional(devotionalId: String?) {
        // First select the tab so it's ready when user goes back
        binding.bottomNav.selectedItemId = R.id.nav_devotionals

        // If we have a specific ID, open that devotional details directly
        devotionalId?.toIntOrNull()?.let { id ->
            val intent = Intent(this, DevotionalDetailsActivity::class.java).apply {
                putExtra("devotional_id", id)
            }
            startActivity(intent)
        }
    }
    
    private fun navigateToAnnouncements() {
        binding.bottomNav.selectedItemId = R.id.nav_announcements
        loadFragment(AnnouncementsFragment())
    }
    
    private fun showNotificationDialog(title: String?, message: String?) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title ?: "Notification")
            .setMessage(message ?: "You have a new notification")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun handleDeepLink(intent: Intent?) {
        intent?.let {
            val data = it.data
            if (data != null && data.scheme == "altarfunds" && data.host == "devotional") {
                // Extract devotional ID from deep link
                val devotionalId = data.pathSegments.getOrNull(0)
                devotionalId?.let { id ->
                    // Navigate to specific devotional
                    val devotionalIdInt = id.toIntOrNull()
                    if (devotionalIdInt != null) {
                        navigateToDevotional(devotionalIdInt.toString())
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle new intent for deep links when app is already running
        handleDeepLink(intent)
        handleNotificationIntent(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, enable notifications and initialize background work
                    showToast("Notification permission granted")
                    initializeBackgroundWork()
                } else {
                    // Permission denied, show rationale
                    showNotificationPermissionRationale()
                }
            }
        }
    }
    
    private fun showNotificationPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Notification Permission Required")
            .setMessage("This app needs notification permission to send you important updates about new devotionals, announcements, and church activities.")
            .setPositiveButton("Grant Permission") { _, _ ->
                // Request permission again
                checkNotificationPermission()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
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
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun loadChurchTheme() {
        viewModel.loadThemeColors()
        viewModel.themeColors.observe(this) { result ->
            when (result) {
                is Resource.Success -> ThemeManager.applyChurchTheme(this, result.data)
                is Resource.Error   -> { /* keep default colours */ }
                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply theme in case it was loaded after the initial layout pass
        ThemeManager.getCurrentTheme()?.let { ThemeManager.applyChurchTheme(this, it) }
    }
}
