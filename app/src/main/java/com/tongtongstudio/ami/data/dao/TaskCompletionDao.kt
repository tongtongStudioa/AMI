package com.tongtongstudio.ami.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tongtongstudio.ami.data.datatables.TaskCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {

    @Insert
    suspend fun insert(completion: TaskCompletion)

    @Delete
    suspend fun delete(completion: TaskCompletion)

    @Update
    suspend fun update(completion: TaskCompletion)
    @Query("DELETE FROM task_completion_table " +
            "WHERE parent_task_id = :parentTaskId ")
    suspend fun deleteLastTaskCompletion(parentTaskId: Long)

    @Query("SELECT * FROM task_completion_table " +
            "WHERE parent_task_id = :parentTaskId " +
            "ORDER BY completionDate DESC " +
            "LIMIT 1")
    fun getLastTaskCompletion(parentTaskId: Long): Flow<TaskCompletion?>
}