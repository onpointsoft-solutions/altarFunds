package com.altarfunds.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val message: String,
    val notification_type: String,
    val status: String,
    val created_at: String,
    val data: String?, // JSON string
    val is_read: Boolean = false,
    val is_synced: Boolean = true
)
