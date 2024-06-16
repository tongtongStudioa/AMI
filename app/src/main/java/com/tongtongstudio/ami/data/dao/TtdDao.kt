package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.*
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
                "ORDER BY task_due_date/8640000 ASC, deadline/8640000 ASC, startDate ASC, priority DESC, estimatedTime DESC"
    )
    fun getLaterTasks(endOfDay: Long): Flow<List<TaskWithSubTasks>>

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE isCompleted == 0 AND (task_due_date BETWEEN :endOfDay AND :endOfDayFilter OR startDate BETWEEN :endOfDay AND :endOfDayFilter) " +
                "ORDER BY task_due_date/8640000 ASC, deadline/8640000 ASC, startDate ASC, priority DESC, estimatedTime DESC"
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
                "ORDER BY isCompleted, task_due_date/8640000 ASC, priority DESC, importance DESC, urgency DESC, estimatedTime DESC"
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
                "ORDER BY isCompleted ASC, task_due_date/8640000 ASC, estimatedTime ASC, skillLevel DESC, isRecurring DESC, priority ASC, urgency DESC, importance DESC"
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
                "ORDER BY isCompleted ASC, task_due_date/8640000 ASC, estimatedTime DESC, priority DESC, importance DESC, deadline ASC, skillLevel ASC"
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
                "AND (startDate < :endOfDay AND isCompleted == 0 AND isRecurring == 0 " +    //startDate BETWEEN :startOfDay AND :endOfDay
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND isCompleted == 0 " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY isCompleted ASC, task_due_date/8640000 ASC, estimatedTime ASC, priority DESC, skillLevel ASC, urgency DESC, importance DESC, isRecurring ASC"
    )
    fun getTasksOrderByCreatorSort(
        hideCompleted: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<TaskWithSubTasks>>

    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE parent_task_id = :parentId " +
                "ORDER BY priority ASC, estimatedTime ASC, skillLevel ASC, urgency DESC, importance DESC, isRecurring ASC"
    )
    fun getSubTasks(parentId: Long): Flow<List<Ttd>>

    @Query("SELECT * FROM thing_to_do_table WHERE task_due_date < :todayDate AND isRecurring ORDER BY task_due_date ASC")
    suspend fun getMissedRecurringTasks(todayDate: Long): List<Ttd>


    // ***********  Statistics *********** //
    @Transaction
    @Query("SELECT * FROM thing_to_do_table WHERE isCompleted ORDER BY completionDate DESC, task_due_date DESC, priority DESC")
    fun getCompletedTasks(): Flow<List<TaskWithSubTasks>>

    @Query("SELECT COUNT(*) FROM thing_to_do_table WHERE isCompleted")
    fun getCompletedTasksCount(): Flow<Int>


    @Query("SELECT COUNT(*) FROM thing_to_do_table WHERE categoryId = :categoryId ")
    fun getCategoryCompletedTasksCount(categoryId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM thing_to_do_table WHERE parent_task_id IS NULL AND isCompleted AND type = 'PROJECT'")
    fun getCompletedProjectsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM thing_to_do_table WHERE parent_task_id IS NULL AND isCompleted AND type = 'PROJECT' AND categoryId = :categoryId ")
    fun getCategoryCompletedProjectsCount(categoryId: Long): Flow<Int>

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

    @Query("SELECT round(1.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*) * 100,1) FROM thing_to_do_table ")
    fun getTotalAchievementRate(): Flow<Float>

    @Query("SELECT round(1.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*) * 100,1)  FROM thing_to_do_table WHERE categoryId = :categoryId")
    fun getCategoryTasksAchievementRate(categoryId: Long): Flow<Float>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE parent_task_id IS NULL AND type = 'PROJECT'"
    )
    fun getAllProjectsAchievementRate(): Flow<Float>

    @Query(
        "SELECT round(CASE WHEN COUNT(*) != 0 THEN 100.0 * COUNT(isCompleted) / COUNT(*) END,1)  " +
                "FROM thing_to_do_table " +
                "WHERE parent_task_id IS NULL AND isCompleted AND type = 'PROJECT' AND categoryId = :categoryId"
    )
    fun getCategoryProjectsAchievementRate(categoryId: Long): Flow<Float>

    /**
     * Function to retrieve completed tasks count of a certain category by day in a period.
     *
     * CompletionDate/ 8640 000 (number of milliseconds in a day to not take in count time)
     */
    @Query(
        "SELECT completionDate, COUNT(*) as completedCount " +
                "FROM thing_to_do_table " +
                "WHERE categoryId = :categoryId AND completionDate BETWEEN :startDate AND :endDate AND isCompleted " +
                "GROUP BY completionDate/ 86400000"
    )
    fun getCompletedCategoryTasksByPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<TtdAchieved?>?>

    /**
     * Function to retrieve all completed tasks count by day in a period.
     *
     * CompletionDate/ 8640 000 (number of milliseconds in a day to not take in count time)
     *
     * @return Flow<List<TtdAchieved>> with completion date day and count
     */
    @Query(
        "SELECT completionDate, COUNT(*) as completedCount " +
                "FROM thing_to_do_table " +
                "WHERE completionDate BETWEEN :startDate AND :endDate AND isCompleted " +
                "GROUP BY completionDate/ 86400000"
    )
    fun getCompletedTasksByPeriod(startDate: Long, endDate: Long): Flow<List<TtdAchieved?>?>

    // TODO: update this method to get actual real time worked ...
    @Query("SELECT SUM(actualWorkTime) FROM thing_to_do_table WHERE isCompleted AND type != 'PROJECT'")
    fun getTotalTimeWorked(): Flow<Long>

    @Query("SELECT SUM(actualWorkTime) FROM thing_to_do_table WHERE categoryId = :categoryId AND isCompleted AND type != 'PROJECT'")
    fun getSumCategoryTimeWorked(categoryId: Long): Flow<Long>

    @Query(
        "SELECT title, actualWorkTime as totalTimeWorked " +
                "FROM thing_to_do_table " +
                "WHERE categoryId = :categoryId AND isCompleted AND type != 'PROJECT'"
    )
    fun getRateTimeWorkedPerTask(categoryId: Long): Flow<List<TimeWorkedDistribution>>

    /**
     * To retrieve sum time worked by tasks grouped by category (even if tasks haven't a category).
     */
    @Query(
        "SELECT category_title as title, SUM(actualWorkTime) as totalTimeWorked " +
                "FROM thing_to_do_table " +
                "LEFT JOIN category ON thing_to_do_table.categoryId = category_id " +
                "WHERE isCompleted AND type != 'PROJECT'" +
                "GROUP BY category_id"
    )
    fun getTimeWorkedPerCategory(): Flow<List<TimeWorkedDistribution>>

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
    fun getAccuracyRateOfEstimatedWorkTime(errorPercent: Float): Flow<Float>

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
                "WHERE task_due_date BETWEEN :startDate AND :endDate " +
                "GROUP BY task_due_date /8640000"
    )
    fun getAccuracyRateOfEstimatedWorkTimeByPeriod(
        startDate: Long,
        endDate: Long,
        errorPercent: Float
    ): Flow<List<Float>>

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
    fun getCategoryAccuracyRateOfEstimatedWorkTime(
        categoryId: Long,
        errorPercent: Float
    ): Flow<Float>

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
                "WHERE categoryId = :categoryId AND task_due_date BETWEEN :startDate AND :endDate " +
                "GROUP BY task_due_date /8640000"
    )
    fun getCategoryAccuracyRateOfEstimatedWorkTimeByPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long,
        errorPercent: Float
    ): Flow<List<Float>>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completedOnTime THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted"
    )
    fun getOnTimeCompletionTasksRate(): Flow<Float>

    // TODO: get on time completion by week or by month
    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completedOnTime THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted AND task_due_date BETWEEN :startDate AND :endDate " +
                "GROUP BY task_due_date /8640000"
    )
    fun getOnTimeCompletionTasksRateByPeriod(startDate: Long, endDate: Long): Flow<List<Float>>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completedOnTime THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted AND categoryId = :categoryId"
    )
    fun getOnTimeCompletionCategoryTasksRate(categoryId: Long): Flow<Float>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completedOnTime THEN 1 END) / COUNT(*),1) " +
                "FROM thing_to_do_table " +
                "WHERE isCompleted AND categoryId = :categoryId AND task_due_date BETWEEN :startDate AND :endDate " +
                "GROUP BY task_due_date /8640000"
    )
    fun getOnTimeCompletionTasksRateByCategoryAndPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<Float>>

    @Query("SELECT title, MAX(maxStreak) as streakInfo FROM thing_to_do_table WHERE isRecurring LIMIT 1")
    fun getMaxStreakTask(): Flow<TtdStreakInfo>

    @Query("SELECT title, MAX(maxStreak) as streakInfo FROM thing_to_do_table WHERE isRecurring AND categoryId = :categoryId LIMIT 1")
    fun getMaxStreakCategoryTask(categoryId: Long): Flow<TtdStreakInfo>

    @Query("SELECT title, MAX(currentStreak) as streakInfo FROM thing_to_do_table WHERE isRecurring LIMIT 1")
    fun getCurrentMaxStreakTask(): Flow<TtdStreakInfo>

    @Query("SELECT title, MAX(currentStreak) as streakInfo FROM thing_to_do_table WHERE isRecurring AND categoryId = :categoryId LIMIT 1")
    fun getCurrentMaxStreakCategoryTask(categoryId: Long): Flow<TtdStreakInfo>

    @Query(
        "SELECT round(AVG(CASE WHEN isRecurring AND totalRepetitionCount != 0 THEN 100.0 * successCount / totalRepetitionCount ELSE NULL END),1) " +
                "FROM thing_to_do_table " +
                "WHERE isRecurring"
    )
    fun getHabitCompletionRate(): Flow<Float>

    @Query(
        "SELECT round(AVG(CASE WHEN totalRepetitionCount != 0 THEN 100.0 * successCount / totalRepetitionCount ELSE NULL END),1) " +
                "FROM thing_to_do_table " +
                "WHERE isRecurring AND categoryId = :categoryId"
    )
    fun getCategoryHabitCompletionRate(categoryId: Long): Flow<Float>

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

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0) " +
                "AND parent_task_id IS NULL " +
                "AND type = 'PROJECT' " +
                "ORDER BY isCompleted ASC, task_due_date/8640000 ASC, estimatedTime ASC, priority DESC, skillLevel ASC, urgency DESC, importance DESC, isRecurring ASC"
    )
    fun getProjects(hideCompleted: Boolean): Flow<List<TaskWithSubTasks>>

    @Query("SELECT * FROM thing_to_do_table WHERE isCompleted == 0")
    fun getPotentialProject(): Flow<List<Ttd>>

    @Transaction
    @Query(
        "SELECT * FROM thing_to_do_table " +
                "WHERE isRecurring " +
                "ORDER BY estimatedTime ASC, priority ASC, skillLevel ASC, urgency DESC, importance DESC"
    )
    fun getRecurringTasks(): Flow<List<TaskWithSubTasks>>

    @Query("SELECT * FROM thing_to_do_table WHERE id_ttd = :parentTaskId LIMIT 1")
    suspend fun getComposedTask(parentTaskId: Long): TaskWithSubTasks

}