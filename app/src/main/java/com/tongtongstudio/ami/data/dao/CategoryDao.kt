package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.CategoryTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Transaction
    @Query("SELECT * FROM Category WHERE category_id = :categoryId")
    fun getCategoryTasks(categoryId: Long): Flow<List<CategoryTasks>>

    @Query("SELECT * FROM Category ORDER BY category_id DESC")
    fun getCategories(): Flow<List<Category>>

    @Query("SELECT * FROM Category WHERE category_id = :id LIMIT 1")
    suspend fun getById(id: Long): Category

    @Query("SELECT * FROM Category WHERE category_title = :title LIMIT 1")
    suspend fun getByTitle(title: String): Category?

    @Insert
    suspend fun insertMultipleCategories(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}