package com.altarfunds.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "church")
data class ChurchEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val code: String,
    val description: String,
    val logo: String?,
    val is_verified: Boolean,
    val is_active: Boolean,
    val phone_number: String,
    val email: String,
    val website: String?,
    val address: String,
    val city: String,
    val county: String,
    val country: String,
    val is_synced: Boolean = true
)
