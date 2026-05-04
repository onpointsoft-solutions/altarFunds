package com.sanctum.member.data

import com.sanctum.member.models.*
import com.sanctum.member.network.RealtimeManager
import com.sanctum.member.utils.OptimizedTokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeRepository @Inject constructor(
    private val realtimeManager: RealtimeManager,
    private val tokenManager: OptimizedTokenManager
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Real-time data state
    private val _latestAnnouncement = MutableStateFlow<Announcement?>(null)
    val latestAnnouncement: Flow<Announcement?> = _latestAnnouncement.asStateFlow()
    
    private val _latestDevotional = MutableStateFlow<Devotional?>(null)
    val latestDevotional: Flow<Devotional?> = _latestDevotional.asStateFlow()
    
    private val _unreadNotificationsCount = MutableStateFlow(0)
    val unreadNotificationsCount: Flow<Int> = _unreadNotificationsCount.asStateFlow()
    
    private val _givingUpdates = MutableStateFlow<List<GivingTransaction>>(emptyList())
    val givingUpdates: Flow<List<GivingTransaction>> = _givingUpdates.asStateFlow()
    
    // Connection status
    val connectionStatus = realtimeManager.connectionState
    
    init {
        // Start listening to real-time events when user is logged in
        scope.launch {
            tokenManager.getIsLoggedInFlow().collect { isLoggedIn ->
                if (isLoggedIn) {
                    startRealtimeListening()
                } else {
                    stopRealtimeListening()
                }
            }
        }
    }
    
    private fun startRealtimeListening() {
        realtimeManager.connect()
        
        // Listen to different event types
        scope.launch {
            realtimeManager.notificationFlow.collect { event ->
                handleNotificationEvent(event)
            }
        }
        
        scope.launch {
            realtimeManager.announcementFlow.collect { event ->
                handleAnnouncementEvent(event)
            }
        }
        
        scope.launch {
            realtimeManager.devotionalFlow.collect { event ->
                handleDevotionalEvent(event)
            }
        }
        
        scope.launch {
            realtimeManager.givingFlow.collect { event ->
                handleGivingEvent(event)
            }
        }
    }
    
    private fun stopRealtimeListening() {
        realtimeManager.disconnect()
        clearRealtimeData()
    }
    
    private fun handleNotificationEvent(event: RealtimeManager.RealtimeEvent) {
        try {
            when (event.data) {
                is PushNotification -> {
                    // Update unread count
                    val currentCount = _unreadNotificationsCount.value
                    _unreadNotificationsCount.value = currentCount + 1
                    
                    // Handle notification type
                    when (event.data.notificationType) {
                        "announcement_posted" -> {
                            // Trigger announcement refresh
                            // This could trigger a repository refresh
                        }
                        "devotional_new" -> {
                            // Trigger devotional refresh
                        }
                        "giving_update" -> {
                            // Trigger giving data refresh
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle parsing errors
        }
    }
    
    private fun handleAnnouncementEvent(event: RealtimeManager.RealtimeEvent) {
        try {
            when (event.data) {
                is Announcement -> {
                    _latestAnnouncement.value = event.data
                }
            }
        } catch (e: Exception) {
            // Handle parsing errors
        }
    }
    
    private fun handleDevotionalEvent(event: RealtimeManager.RealtimeEvent) {
        try {
            when (event.data) {
                is Devotional -> {
                    _latestDevotional.value = event.data
                }
            }
        } catch (e: Exception) {
            // Handle parsing errors
        }
    }
    
    private fun handleGivingEvent(event: RealtimeManager.RealtimeEvent) {
        try {
            when (event.data) {
                is GivingTransaction -> {
                    val currentUpdates = _givingUpdates.value.toMutableList()
                    currentUpdates.add(0, event.data) // Add to beginning
                    _givingUpdates.value = currentUpdates
                }
                is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    _givingUpdates.value = event.data as List<GivingTransaction>
                }
            }
        } catch (e: Exception) {
            // Handle parsing errors
        }
    }
    
    private fun clearRealtimeData() {
        _latestAnnouncement.value = null
        _latestDevotional.value = null
        _unreadNotificationsCount.value = 0
        _givingUpdates.value = emptyList()
    }
    
    // Public methods for UI to interact with real-time data
    
    fun markNotificationsAsRead() {
        _unreadNotificationsCount.value = 0
    }
    
    fun clearGivingUpdates() {
        _givingUpdates.value = emptyList()
    }
    
    fun isConnected(): Boolean {
        return realtimeManager.isConnected()
    }
    
    // Combined flow for UI components that need multiple data sources
    fun getDashboardRealtimeData(): Flow<DashboardRealtimeData> {
        return combine(
            latestAnnouncement,
            latestDevotional,
            unreadNotificationsCount,
            givingUpdates
        ) { announcement, devotional, notificationCount, givingData ->
            DashboardRealtimeData(
                latestAnnouncement = announcement,
                latestDevotional = devotional,
                unreadNotificationsCount = notificationCount,
                recentGivingUpdates = givingData
            )
        }
    }
    
    data class DashboardRealtimeData(
        val latestAnnouncement: Announcement?,
        val latestDevotional: Devotional?,
        val unreadNotificationsCount: Int,
        val recentGivingUpdates: List<GivingTransaction>
    )
}
