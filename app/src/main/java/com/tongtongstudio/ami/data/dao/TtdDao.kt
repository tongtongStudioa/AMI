package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import kotlinx.coroutines.flow.Flow

@Dao
interface TtdDao {

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
            SortOrder.BY_EAT_THE_FROG -> getTasksOrderByEatTheFrogSort(
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
                "ORDER BY task_due_date ASC, deadline ASC, startDate ASC, priority DESC, estimatedTime DESC"
    )
    fun getLaterTasks(endOfDay: Long): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE isCompleted == 0 AND (task_due_date BETWEEN :endOfDay AND :endOfDayFilter OR startDate BETWEEN :endOfDay AND :endOfDayFilter) " +
                "ORDER BY task_due_date ASC, deadline ASC, startDate ASC, priority DESC, estimatedTime DESC"
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
                "ORDER BY isCompleted, startDate ASC, priority DESC, importance DESC, urgency DESC, estimatedTime DESC"
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
                "ORDER BY isCompleted ASC, startDate ASC, estimatedTime ASC, skillLevel DESC, isRecurring DESC, priority ASC, urgency DESC, importance DESC"
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
                "ORDER BY isCompleted ASC, startDate ASC, estimatedTime DESC, priority DESC, importance DESC, deadline ASC, skillLevel ASC"
    )
    fun getTasksOrderByEatTheFrogSort(
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
                "ORDER BY isCompleted ASC, startDate ASC, estimatedTime ASC, priority DESC, skillLevel ASC, urgency DESC, importance DESC, isRecurring ASC"
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

    @Query("SELECT * FROM thing_to_do_table WHERE task_due_date < :todayDate AND isRecurring == 1 ORDER BY task_due_date ASC")
    suspend fun getMissedRecurringTasks(todayDate: Long): List<Ttd>


    // ***********  Statistics *********** //
    @Transaction
    @Query("SELECT * FROM thing_to_do_table WHERE isCompleted ORDER BY completionDate DESC, task_due_date DESC, priority DESC")
    fun getCompletedTasks(): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query("SELECT COUNT(*) FROM thing_to_do_table WHERE isCompleted")
    suspend fun getCompletedTasksCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM thing_to_do_table WHERE categoryId = :categoryId ")
    fun getCategoryCompletedTasksCount(categoryId: Long): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM thing_to_do_table WHERE parent_task_id IS NULL AND isCompleted AND type = 'PROJECT'")
    suspend fun getCompletedProjectsCount(): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM thing_to_do_table WHERE parent_task_id IS NULL AND isCompleted AND type = 'PROJECT' AND categoryId = :categoryId ")
    fun getCategoryCompletedProjectsCount(categoryId: Long): Int

    @Transaction
    @Query(
        "SELECT COUNT(*) FROM thing_to_do_table " +
                "WHERE isCompleted == 0 AND task_due_date > :endDate AND task_due_date < :endDateFilter"
    )
    fun getUpcomingTasksCountFilter(endDate: Long, endDateFilter: Long): Flow<Int>

    @Transaction
    @Query(
        "SELECT COUNT(*) FROM thing_to_do_table " +
                "WHERE isCompleted == 0 AND task_due_date > :endDate"
    )
    fun getUpcomingTasksCount(endDate: Long): Flow<Int>

    @Transaction
    @Query("SELECT round(1.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*) * 100,1) FROM thing_to_do_table ")
    suspend fun getTotalAchievementRate(): Float

    @Transaction
    @Query("SELECT round(1.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*) * 100,1)  FROM thing_to_do_table WHERE categoryId = :categoryId")
    suspend fun getCategoryTasksAchievementRate(categoryId: Long): Float

    @Transaction
    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE parent_task_id IS NULL AND type = 'PROJECT'"
    )
    suspend fun getAllProjectsAchievementRate(): Float

    @Transaction
    @Query(
        "SELECT round(CASE WHEN COUNT(*) != 0 THEN 100.0 * COUNT(isCompleted) / COUNT(*) END,1)  " +
                "FROM thing_to_do_table " +
                "WHERE parent_task_id IS NULL AND isCompleted AND type = 'PROJECT' AND categoryId = :categoryId"
    )
    suspend fun getCategoryProjectsAchievementRate(categoryId: Long): Float

    @Transaction
    @Query(
        "SELECT COUNT(*) " +
                "FROM thing_to_do_table " +
                "WHERE categoryId = :categoryId AND " +
                "isCompleted AND task_due_date BETWEEN :startDate AND :endDate"
    )
    suspend fun getCompletedCategoryTasksByPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Int

    @Transaction
    @Query(
        "SELECT COUNT(*) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted AND task_due_date BETWEEN :startDate AND :endDate"
    )
    suspend fun getCompletedTasksByPeriod(startDate: Long, endDate: Long): Int

    @Query("SELECT SUM(actualWorkTime) FROM thing_to_do_table WHERE isCompleted")
    suspend fun getTotalTimeWorked(): Long

    @Query("SELECT SUM(actualWorkTime) FROM thing_to_do_table WHERE categoryId = :categoryId AND isCompleted")
    suspend fun getSumCategoryTimeWorked(categoryId: Long): Long

    // TODO: clear intermediate class from this file
    /*@Query(
        "SELECT title, (actualWorkTime * 1.0 / Total.timeWorked) * 100 AS timePercentage " +
                "FROM thing_to_do_table " +
                "INNER JOIN " +
                "( SELECT SUM(actualWorkTime) as timeWorked FROM thing_to_do_table WHERE categoryId = :categoryId) AS Total" +
                "WHERE categoryId = :categoryId"
    )
    suspend fun getRateTimeWorkedPerTask(categoryId: Long): List<TimePercentageTask>

    @Query(
        "SELECT title, (actualWorkTime * 1.0 / Total.timeWorked) * 100 AS timePercentage " +
                "FROM thing_to_do_table " +
                "INNER JOIN " +
                "( SELECT SUM(actualWorkTime) as timeWorked FROM thing_to_do_table WHERE categoryId = :categoryId) AS Total"
    )
    suspend fun getRateTimeWorkedPerCategory(categoryId: Long): List<TimePercentageTask>
    data class TimePercentageTask(val title: String, val timePercentage: Float)*/

    /**
     * Get accuracy rate of estimated work time for all tasks completed.
     * If the current working time is equal to estimated time +- errorPercent so the task's time work is well estimated.
     */
    @Query(
        "SELECT " +
                "round(1.0 * COUNT(" +
                "CASE " +
                "WHEN " +
                "isCompleted AND " +
                "actualWorkTime BETWEEN estimatedTime * (1-:errorPercent) AND estimatedTime * (1+:errorPercent) " +
                "THEN 1 END) " +
                "/ COUNT(CASE WHEN isCompleted THEN 1 END) * 100,1)" +
                "FROM thing_to_do_table"
    )
    suspend fun getAccuracyRateOfEstimatedWorkTime(errorPercent: Float): Float

    @Query(
        "SELECT " +
                "round(1.0 * COUNT(" +
                "CASE " +
                "WHEN " +
                "isCompleted AND " +
                "actualWorkTime BETWEEN estimatedTime * (1-:errorPercent) AND estimatedTime * (1+:errorPercent) " +
                "THEN 1 END) " +
                "/ COUNT(CASE WHEN isCompleted THEN 1 END) * 100,1)" +
                "FROM thing_to_do_table " +
                "WHERE task_due_date BETWEEN :startDate AND :endDate"
    )
    suspend fun getAccuracyRateOfEstimatedWorkTimeByPeriod(
        startDate: Long,
        endDate: Long,
        errorPercent: Float
    ): Float

    @Query(
        "SELECT " +
                "round(1.0 * COUNT(" +
                "CASE " +
                "WHEN " +
                "isCompleted AND " +
                "actualWorkTime BETWEEN estimatedTime * (1-:errorPercent) AND estimatedTime * (1+:errorPercent) " +
                "THEN 1 END) " +
                "/ COUNT(CASE WHEN isCompleted THEN 1 END) * 100,1)" +
                "FROM thing_to_do_table " +
                "WHERE categoryId = :categoryId"
    )
    suspend fun getCategoryAccuracyRateOfEstimatedWorkTime(
        categoryId: Long,
        errorPercent: Float
    ): Float

    @Query(
        "SELECT " +
                "round(1.0 * COUNT(" +
                "CASE " +
                "WHEN " +
                "isCompleted AND " +
                "actualWorkTime BETWEEN estimatedTime * (1-:errorPercent) AND estimatedTime * (1+:errorPercent) " +
                "THEN 1 END) " +
                "/ COUNT(CASE WHEN isCompleted THEN 1 END) * 100,1)" +
                "FROM thing_to_do_table " +
                "WHERE categoryId = :categoryId AND task_due_date BETWEEN :startDate AND :endDate"
    )
    suspend fun getCategoryAccuracyRateOfEstimatedWorkTimeByPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long,
        errorPercent: Float
    ): Float

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completedOnTime THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted"
    )
    suspend fun getOnTimeCompletionTasksRate(): Float

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completedOnTime THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted AND task_due_date BETWEEN :startDate AND :endDate"
    )
    suspend fun getOnTimeCompletionTasksRateByPeriod(startDate: Long, endDate: Long): Float

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completedOnTime THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted AND categoryId = :categoryId"
    )
    suspend fun getOnTimeCompletionCategoryTasksRate(categoryId: Long): Float

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completedOnTime THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted AND categoryId = :categoryId AND task_due_date BETWEEN :startDate AND :endDate"
    )
    suspend fun getOnTimeCompletionTasksRateByCategoryAndPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Float

    @Query("SELECT *, MAX(maxStreak) FROM thing_to_do_table WHERE isRecurring LIMIT 1")
    suspend fun getMaxStreakTask(): Ttd

    @Query("SELECT *, MAX(currentStreak) FROM thing_to_do_table WHERE isRecurring LIMIT 1")
    suspend fun getMaxCurrentStreakTask(): Ttd

    @Query(
        "SELECT round(AVG(CASE WHEN isRecurring AND totalRepetitionCount != 0 THEN 100.0 * successCount / totalRepetitionCount ELSE NULL END),1) " +
                "FROM thing_to_do_table " +
                "WHERE isRecurring"
    )
    suspend fun getHabitCompletionRate(): Float

    @Query(
        "SELECT round(AVG(CASE WHEN totalRepetitionCount != 0 THEN 100.0 * successCount / totalRepetitionCount ELSE NULL END),1) " +
                "FROM thing_to_do_table " +
                "WHERE isRecurring AND categoryId = :categoryId"
    )
    suspend fun getCategoryHabitCompletionRate(categoryId: Long): Float

    // ************ Base Method *************** //

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

    // TODO: use type to retrieve projects
    @Transaction
    @Query("SELECT * FROM thing_to_do_table WHERE parent_task_id IS NULL AND isCompleted == 0 AND type = 'PROJECT'")
    fun getProjects(): Flow<List<TaskWithSubTasks>>

    @Query("SELECT * FROM thing_to_do_table WHERE isCompleted == 0")
    fun getPotentialProject(): Flow<List<Ttd>>

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE isRecurring " +
                "ORDER BY estimatedTime ASC, priority ASC, skillLevel ASC, urgency DESC, importance DESC"
    )
    fun getRecurringTasks(): Flow<List<TaskWithSubTasks>>

}