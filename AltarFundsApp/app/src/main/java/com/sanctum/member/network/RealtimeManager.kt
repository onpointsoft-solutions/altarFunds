package com.sanctum.member.network

import android.util.Log
import com.google.gson.Gson
import com.sanctum.member.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    
    // Real-time event flows
    private val _notificationFlow = MutableSharedFlow<RealtimeEvent>()
    val notificationFlow: SharedFlow<RealtimeEvent> = _notificationFlow.asSharedFlow()
    
    private val _announcementFlow = MutableSharedFlow<RealtimeEvent>()
    val announcementFlow: SharedFlow<RealtimeEvent> = _announcementFlow.asSharedFlow()
    
    private val _devotionalFlow = MutableSharedFlow<RealtimeEvent>()
    val devotionalFlow: SharedFlow<RealtimeEvent> = _devotionalFlow.asSharedFlow()
    
    private val _givingFlow = MutableSharedFlow<RealtimeEvent>()
    val givingFlow: SharedFlow<RealtimeEvent> = _givingFlow.asSharedFlow()
    
    // Connection state
    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()
    
    enum class ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED, ERROR
    }
    
    data class RealtimeEvent(
        val type: String,
        val data: Any,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    fun connect() {
        try {
            val token = tokenManager.getToken()
            if (token == null) {
                Log.e("RealtimeManager", "Cannot connect: No auth token")
                return
            }
            
            val client = OkHttpClient.Builder().build()
            val request = Request.Builder()
                .url("wss://backend.sanctum.co.ke/ws/notifications/?token=$token")
                .build()
            
            _connectionState.tryEmit(ConnectionState.CONNECTING)
            
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("RealtimeManager", "WebSocket connected")
                    _connectionState.tryEmit(ConnectionState.CONNECTED)
                    
                    // Send initial connection message
                    webSocket.send(gson.toJson(mapOf(
                        "type" to "ping",
                        "timestamp" to System.currentTimeMillis()
                    )))
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val event = parseMessage(text)
                        handleRealtimeEvent(event)
                    } catch (e: Exception) {
                        Log.e("RealtimeManager", "Error parsing message: $text", e)
                    }
                }
                
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("RealtimeManager", "WebSocket closing: $code - $reason")
                    _connectionState.tryEmit(ConnectionState.DISCONNECTED)
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("RealtimeManager", "WebSocket error", t)
                    _connectionState.tryEmit(ConnectionState.ERROR)
                    
                    // Attempt reconnection after delay
                    scope.launch {
                        delay(5000)
                        connect()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("RealtimeManager", "Failed to connect WebSocket", e)
            _connectionState.tryEmit(ConnectionState.ERROR)
        }
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.tryEmit(ConnectionState.DISCONNECTED)
    }
    
    private fun parseMessage(text: String): RealtimeEvent {
        val json = JSONObject(text)
        return RealtimeEvent(
            type = json.getString("type"),
            data = json.get("data"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
    
    private fun handleRealtimeEvent(event: RealtimeEvent) {
        when (event.type) {
            "notification" -> _notificationFlow.tryEmit(event)
            "announcement" -> _announcementFlow.tryEmit(event)
            "devotional" -> _devotionalFlow.tryEmit(event)
            "giving_update" -> _givingFlow.tryEmit(event)
            "ping" -> {
                // Respond to server ping
                webSocket?.send(gson.toJson(mapOf(
                    "type" to "pong",
                    "timestamp" to System.currentTimeMillis()
                )))
            }
        }
    }
    
    fun isConnected(): Boolean {
        return webSocket != null && _connectionState.replayCache.lastOrNull() == ConnectionState.CONNECTED
    }
}
