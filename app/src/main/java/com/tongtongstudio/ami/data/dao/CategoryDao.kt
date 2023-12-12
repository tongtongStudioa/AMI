package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.datatables.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM Category ORDER BY category_id DESC")
    fun getCategories(): Flow<List<Category>>

    @Query("SELECT * FROM Category WHERE category_id = :id LIMIT 1")
    suspend fun get(id: Long): Category

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}