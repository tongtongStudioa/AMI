package com.tongtongstudio.ami.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tongtongstudio.ami.data.datatables.Assessment
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM Assessment WHERE parent_id = :taskId ORDER BY assessment_id ASC")
    fun getTaskAssessments(taskId: Long): Flow<MutableList<Assessment>>

    @Query("SELECT * FROM Assessment WHERE assessment_id = :id LIMIT 1")
    suspend fun get(id: Long): Assessment

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(assessment: Assessment): Long

    @Update
    suspend fun update(assessment: Assessment)

    @Delete
    suspend fun delete(assessment: Assessment)

    @Query("SELECT * FROM Assessment WHERE parent_id is NULL ORDER BY assessment_due_date")
    fun getGlobalGoals(): Flow<List<Assessment>>
}