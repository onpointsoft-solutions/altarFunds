package com.sanctum.member.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import com.sanctum.member.ui.MainActivity
import com.sanctum.member.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {
    
    private val app by lazy { MemberApp.getInstance() }
    private val SPLASH_DELAY = 2000L // 2 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Check authentication status after splash delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationAndNavigate()
        }, SPLASH_DELAY)
    }
    
    private fun checkAuthenticationAndNavigate() {
        if (app.tokenManager.isLoggedIn.value) {
            // User is logged in, navigate to main app
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is not logged in, navigate to login
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}
