package com.altarfunds.mobile.data.dao

import androidx.room.*
import com.altarfunds.mobile.data.entities.GivingCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GivingCategoryDao {
    
    @Query("SELECT * FROM giving_categories WHERE is_active = 1 ORDER BY display_order ASC")
    fun getActiveCategories(): Flow<List<GivingCategoryEntity>>
    
    @Query("SELECT * FROM giving_categories ORDER BY display_order ASC")
    fun getAllCategories(): Flow<List<GivingCategoryEntity>>
    
    @Query("SELECT * FROM giving_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Int): GivingCategoryEntity?
    
    @Query("SELECT * FROM giving_categories WHERE name LIKE :searchTerm ORDER BY name ASC")
    fun searchCategories(searchTerm: String): Flow<List<GivingCategoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: GivingCategoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<GivingCategoryEntity>)
    
    @Update
    suspend fun updateCategory(category: GivingCategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: GivingCategoryEntity)
    
    @Query("DELETE FROM giving_categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Int)
    
    @Query("DELETE FROM giving_categories")
    suspend fun deleteAllCategories()
    
    @Query("SELECT COUNT(*) FROM giving_categories WHERE is_active = 1")
    suspend fun getActiveCategoryCount(): Int
}
