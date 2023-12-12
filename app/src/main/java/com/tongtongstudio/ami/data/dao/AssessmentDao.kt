package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.datatables.Assessment
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM Assessment WHERE task_id = :taskId ORDER BY assessment_id ASC")
    fun getTaskAssessments(taskId: Long): Flow<List<Assessment>>

    @Query("SELECT * FROM Assessment WHERE assessment_id = :id LIMIT 1")
    suspend fun get(id: Long): Assessment

    @Insert
    suspend fun insert(assessment: Assessment): Long

    @Update
    suspend fun update(assessment: Assessment)

    @Delete
    suspend fun delete(assessment: Assessment)
}