package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import kotlinx.coroutines.flow.Flow

@Dao
interface TtdDao {

    // TODO: Create const val for sortOrder string or use class SortOrder
    fun getTodayTasks(
        sortOrder: SortOrder,
        hideCompleted: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<TaskWithSubTasks>> {
        return when (sortOrder) {
            SortOrder.BY_2MINUTES_RULES -> getTasksOrderBy2minutesRules(
                hideCompleted,
                startOfDay,
                endOfDay
            )
            SortOrder.BY_EISENHOWER_MATRIX -> getTasksOrderByEisenhowerMatrixSort(
                hideCompleted,
                startOfDay,
                endOfDay
            )
            SortOrder.EAT_THE_FROG -> getTasksOrderByCreatorSort(
                hideCompleted,
                startOfDay,
                endOfDay
            )
            SortOrder.BY_CREATOR_SORT -> getTasksOrderByCreatorSort(
                hideCompleted,
                startOfDay,
                endOfDay
            )
            else -> getTasksOrderByCreatorSort(hideCompleted, startOfDay, endOfDay)
        }
    }

    // TODO: add multiple sort after by dueDate, deadline and startDate : like Today's tasks
    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE isCompleted == 0 " +
                "AND (task_due_date > :endOfDay OR startDate > :endOfDay) " +
                "ORDER BY task_due_date ASC, deadline ASC, startDate ASC, priority ASC, estimatedTime DESC"
    )
    fun getLaterTasks(endOfDay: Long): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE isCompleted == 0 AND (task_due_date BETWEEN :endOfDay AND :endOfDayFilter OR startDate BETWEEN :endOfDay AND :endOfDayFilter) " +
                "ORDER BY task_due_date ASC, deadline ASC, startDate ASC, priority ASC, estimatedTime DESC"
    )
    fun getLaterTasksFilter(endOfDay: Long, endOfDayFilter: Long): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0) " +
                "AND (startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND isCompleted == 0 " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY isCompleted ASC,priority DESC, importance DESC, urgency DESC, estimatedTime DESC"
    )
    fun getTasksOrderByEisenhowerMatrixSort(
        hideCompleted: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0) " +
                "AND (startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND isCompleted == 0 " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY isCompleted ASC, estimatedTime ASC, skillLevel DESC, isRecurring DESC, priority ASC, urgency DESC, importance DESC"
    )
    fun getTasksOrderBy2minutesRules(
        hideCompleted: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0) " +
                "AND (startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND isCompleted == 0 " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY isCompleted ASC, estimatedTime ASC, priority DESC, skillLevel ASC, urgency DESC, importance DESC, isRecurring ASC"
    )
    fun getTasksOrderByCreatorSort(
        hideCompleted: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<TaskWithSubTasks>>

    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE parent_task_id = :parentId " +
                "ORDER BY estimatedTime ASC, priority ASC, skillLevel ASC, urgency DESC, importance DESC, isRecurring ASC"
    )
    fun getSubTasks(parentId: Long): Flow<List<Ttd>>

    @Query("SELECT * FROM thing_to_do_table WHERE task_due_date < :todayDate AND isRecurring == 1")
    suspend fun getMissedRecurringTasks(todayDate: Long): List<Ttd>

    @Transaction
    @Query("SELECT * FROM thing_to_do_table WHERE isCompleted == 1 ORDER BY completionDate DESC, task_due_date DESC, priority DESC")
    fun getCompletedTasks(): Flow<List<TaskWithSubTasks>>

    @Query("SELECT AVG(actualWorkTime) FROM thing_to_do_table WHERE isCompleted")
    fun getAverageWorkTime(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Ttd): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTasks(listSubTasks: List<Ttd>)

    @Update
    suspend fun update(task: Ttd)

    @Delete
    suspend fun delete(task: Ttd)

    @Delete
    suspend fun deleteTasks(tasks: List<Ttd>)

    @Query("SELECT * FROM thing_to_do_table WHERE id_ttd = :id LIMIT 1")
    suspend fun getTask(id: Long): Ttd

    @Query("SELECT * FROM thing_to_do_table WHERE parent_task_id IS NULL AND isCompleted == 0")
    fun getTaskComposed(): Flow<List<Ttd>>
}