package com.altarfunds.mobile

import android.app.Application
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.data.AppDatabase
import com.altarfunds.mobile.data.PreferencesManager
import com.altarfunds.mobile.utils.NotificationManager

class AltarFundsApp : Application() {
    
    lateinit var database: AppDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var notificationManager: NotificationManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dependencies
        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        notificationManager = NotificationManager(this)
        
        // Initialize API service
        ApiService.initialize()
    }
}
