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

        tokenManager = TokenManager(this)
        // Pass application context so the interceptor can redirect to LoginActivity on 401
        apiService = RetrofitClient.create(tokenManager, applicationContext)
    }

    companion object {
        private lateinit var instance: MemberApp
        fun getInstance(): MemberApp = instance
    }
}
