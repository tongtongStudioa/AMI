package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.datatables.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM task_table WHERE (isTaskCompleted != :hideCompleted OR isTaskCompleted == 0) AND (taskStartDate BETWEEN :startOfToday AND :endOfToday OR taskDeadline BETWEEN :startOfToday AND :endOfToday OR taskStartDate < :endOfToday AND isTaskCompleted == 0 OR taskDeadline < :endOfToday AND isTaskCompleted == 0)")
    fun getTodayTasks(
        hideCompleted: Boolean,
        startOfToday: Long,
        endOfToday: Long
    ): Flow<List<Task>>

    @Query("SELECT * FROM task_table WHERE isTaskCompleted == 0 AND (taskDeadline > :endOfToday OR taskStartDate > :endOfToday) AND project_id IS NULL")
    fun getLaterTasks(endOfToday: Long): Flow<List<Task>>

    @Query("SELECT * FROM task_table WHERE isTaskCompleted == 0 AND (taskDeadline BETWEEN :endOfToday AND :endOfDayFilter OR taskStartDate BETWEEN :endOfToday AND :endOfDayFilter) AND project_id IS NULL")
    fun getLaterTasksFilter(endOfToday: Long, endOfDayFilter: Long): Flow<List<Task>>

    @Query("SELECT * FROM task_table WHERE isTaskCompleted AND project_id IS NULL ORDER BY taskCompletedDate DESC")
    fun getAllCompletedTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Delete
    suspend fun deleteSubTasks(subTasks: List<Task>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUndoDeletedSubTasks(listSubTasks: List<Task>)

    // to get all tasks completed with their stats
    @Query("SELECT * FROM task_table WHERE isTaskCompleted")
    fun getTasksCompletedStats(): Flow<List<Task>>

    @Query("SELECT COUNT(taskName) FROM task_table WHERE (taskStartDate > :endOfToday OR taskDeadline > :endOfToday) AND isTaskCompleted == 0")
    suspend fun getUpcomingTasksCount(endOfToday: Long): Int

    @Query("SELECT * FROM task_table WHERE taskStartDate < :todayDate AND is_recurring == 1")
    suspend fun getMissedRecurringTasks(todayDate: Long): List<Task>
}