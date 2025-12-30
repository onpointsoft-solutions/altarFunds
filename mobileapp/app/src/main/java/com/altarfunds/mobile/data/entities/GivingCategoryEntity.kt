package com.altarfunds.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "giving_categories")
data class GivingCategoryEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val description: String?,
    val is_tax_deductible: Boolean,
    val is_active: Boolean,
    val monthly_target: Double?,
    val yearly_target: Double?,
    val display_order: Int,
    val church_id: Int,
    val is_synced: Boolean = true
)
