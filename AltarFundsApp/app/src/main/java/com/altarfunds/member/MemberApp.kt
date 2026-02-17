package com.altarfunds.member

import android.app.Application
import com.altarfunds.member.api.ApiService
import com.altarfunds.member.api.RetrofitClient
import com.altarfunds.member.data.local.AppDatabase
import com.altarfunds.member.utils.TokenManager

class MemberApp : Application() {
    
    lateinit var tokenManager: TokenManager
    lateinit var apiService: ApiService
    lateinit var database: AppDatabase
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize TokenManager
        tokenManager = TokenManager(this)
        
        // Initialize API Service
        apiService = RetrofitClient.create(tokenManager)
        
        // Initialize Database
        database = AppDatabase.getDatabase(this)
    }
    
    companion object {
        private lateinit var instance: MemberApp
        
        fun getInstance(): MemberApp {
            return instance
        }
    }
}
