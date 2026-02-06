package com.altarfunds.member

import android.app.Application
import com.altarfunds.member.api.ApiService
import com.altarfunds.member.api.RetrofitClient
import com.altarfunds.member.utils.TokenManager

class MemberApp : Application() {
    
    lateinit var tokenManager: TokenManager
    lateinit var apiService: ApiService
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize TokenManager
        tokenManager = TokenManager(this)
        
        // Initialize API Service
        apiService = RetrofitClient.create(tokenManager)
    }
    
    companion object {
        private lateinit var instance: MemberApp
        
        fun getInstance(): MemberApp {
            return instance
        }
    }
}
