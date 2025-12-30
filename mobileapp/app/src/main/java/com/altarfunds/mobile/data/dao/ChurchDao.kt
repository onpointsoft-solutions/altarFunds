package com.altarfunds.mobile.data.dao

import androidx.room.*
import com.altarfunds.mobile.data.entities.ChurchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChurchDao {
    
    @Query("SELECT * FROM church WHERE id = :churchId")
    suspend fun getChurchById(churchId: String): ChurchEntity?
    
    @Query("SELECT * FROM church WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveChurch(): ChurchEntity?
    
    @Query("SELECT * FROM church WHERE is_active = 1")
    fun getActiveChurches(): Flow<List<ChurchEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChurch(church: ChurchEntity)
    
    @Update
    suspend fun updateChurch(church: ChurchEntity)
    
    @Delete
    suspend fun deleteChurch(church: ChurchEntity)
    
    @Query("DELETE FROM church WHERE id = :churchId")
    suspend fun deleteChurchById(churchId: String)
    
    @Query("DELETE FROM church")
    suspend fun deleteAllChurches()
}
