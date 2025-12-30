package com.altarfunds.mobile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.altarfunds.mobile.models.GivingTransactionResponse

@Entity(tableName = "giving_transactions")
data class GivingTransactionEntity(
    @PrimaryKey
    val transaction_id: String,
    val amount: Double,
    val category_id: Int,
    val category_name: String,
    val status: String,
    val payment_method: String,
    val transaction_date: String,
    val note: String?,
    val is_anonymous: Boolean,
    val created_at: String,
    val updated_at: String,
    val is_synced: Boolean = true
) {
    companion object {
        fun fromResponse(response: GivingTransactionResponse): GivingTransactionEntity {
            return GivingTransactionEntity(
                transaction_id = response.transaction_id,
                amount = response.amount,
                category_id = response.category.id,
                category_name = response.category.name,
                status = response.status,
                payment_method = response.payment_method,
                transaction_date = response.transaction_date,
                note = response.note,
                is_anonymous = false,
                created_at = response.transaction_date,
                updated_at = response.transaction_date
            )
        }
    }
}
