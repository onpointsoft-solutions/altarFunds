package com.altarfunds.mobile.data.dao

import androidx.room.*
import com.altarfunds.mobile.data.entities.GivingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GivingTransactionDao {
    
    @Query("SELECT * FROM giving_transactions ORDER BY created_at DESC")
    fun getAllTransactions(): Flow<List<GivingTransactionEntity>>
    
    @Query("SELECT * FROM giving_transactions WHERE status = 'completed' ORDER BY created_at DESC")
    fun getCompletedTransactions(): Flow<List<GivingTransactionEntity>>
    
    @Query("SELECT * FROM giving_transactions WHERE status = 'pending' ORDER BY created_at DESC")
    fun getPendingTransactions(): Flow<List<GivingTransactionEntity>>
    
    @Query("SELECT * FROM giving_transactions WHERE transaction_id = :transactionId")
    suspend fun getTransactionById(transactionId: String): GivingTransactionEntity?
    
    @Query("SELECT * FROM giving_transactions WHERE category_id = :categoryId ORDER BY created_at DESC")
    fun getTransactionsByCategory(categoryId: Int): Flow<List<GivingTransactionEntity>>
    
    @Query("SELECT * FROM giving_transactions WHERE created_at >= :startDate ORDER BY created_at DESC")
    fun getTransactionsSince(startDate: String): Flow<List<GivingTransactionEntity>>
    
    @Query("SELECT SUM(amount) FROM giving_transactions WHERE status = 'completed'")
    suspend fun getTotalGiving(): Double?
    
    @Query("SELECT SUM(amount) FROM giving_transactions WHERE status = 'completed' AND created_at >= :startDate")
    suspend fun getTotalGivingSince(startDate: String): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: GivingTransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<GivingTransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: GivingTransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: GivingTransactionEntity)
    
    @Query("DELETE FROM giving_transactions WHERE transaction_id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)
    
    @Query("DELETE FROM giving_transactions")
    suspend fun deleteAllTransactions()
    
    @Query("SELECT COUNT(*) FROM giving_transactions WHERE status = 'pending'")
    suspend fun getPendingTransactionCount(): Int
}
