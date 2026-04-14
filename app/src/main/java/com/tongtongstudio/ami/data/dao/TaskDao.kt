package com.tongtongstudio.ami.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.CountSinceLastCompletion
import com.tongtongstudio.ami.data.datatables.IndicatorRateByPeriod
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.data.datatables.TaskRelations
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.ThingToDoDetails
import com.tongtongstudio.ami.data.datatables.TimeWorkedDistribution
import com.tongtongstudio.ami.data.datatables.TtdAchieved
import com.tongtongstudio.ami.data.datatables.TtdStreakInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    fun getTodayTasks(
        sortOrder: SortOrder,
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>> {
        return when (sortOrder) {
            SortOrder.BY_2MINUTES_RULES -> getTasksOrderBy2minutesRules(
                hideCompleted,
                hideLateTasks,
                startOfDay,
                endOfDay
            )

            SortOrder.BY_EISENHOWER_MATRIX -> getTasksOrderByEisenhowerMatrixSort(
                hideCompleted,
                hideLateTasks,
                startOfDay,
                endOfDay
            )

            SortOrder.BY_EAT_THE_FROG -> getTasksOrderByEatTheFrogSort(
                hideCompleted,
                hideLateTasks,
                startOfDay,
                endOfDay
            )

            SortOrder.BY_CREATOR_SORT -> getTasksOrderByCreatorSort(
                hideCompleted,
                hideLateTasks,
                startOfDay,
                endOfDay
            )

            else -> getTasksOrderByCreatorSort(hideCompleted, hideLateTasks, startOfDay, endOfDay)
        }
    }

    // TODO: add multiple sort after by dueDate, deadline and startDate : like Today's tasks
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM thing_to_do_view t " +
                "WHERE (last_completion_status == 0 OR last_completion_status is NULL OR task_recurrence_id IS NOT NULL) AND NOT isDraft " +
                "AND (task_due_date > :endOfDay OR startDate > :endOfDay) " +
                "ORDER BY task_due_date/8640000 ASC, deadline/8640000 ASC, startDate ASC, priority DESC, estimatedWorkingTime DESC"
    )
    fun getLaterTasks(endOfDay: Long): Flow<List<ThingToDo>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM thing_to_do_view t " +
                "WHERE (last_completion_status == 0 OR last_completion_status is NULL OR task_recurrence_id IS NOT NULL) AND NOT isDraft AND (task_due_date BETWEEN :endOfDay AND :endOfDayFilter OR startDate BETWEEN :endOfDay AND :endOfDayFilter) " +
                "ORDER BY task_due_date/8640000 ASC, deadline/8640000 ASC, startDate ASC, priority DESC, estimatedWorkingTime DESC"
    )
    fun getLaterTasks(endOfDay: Long, endOfDayFilter: Long): Flow<List<ThingToDo>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT ttd.* " +
                "FROM thing_to_do_view ttd " +
                //"LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE (last_completion_status != :hideCompleted OR last_completion_status == 0 OR last_completion_status IS NULL OR task_recurrence_id IS NOT NULL) AND NOT ttd.isDraft " +
                "AND (ttd.startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR ttd.task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR ttd.task_due_date < :endOfDay AND (last_completion_status == 0 OR last_completion_status IS NULL OR task_recurrence_id IS NOT NULL) AND NOT :hideLateTasks " +
                "OR ttd.deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY last_completion_status, priority DESC, importance DESC, urgency DESC, estimatedWorkingTime DESC"
    )
    fun getTasksOrderByEisenhowerMatrixSort(
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * " +
                "FROM thing_to_do_view AS t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE (last_completion_status != :hideCompleted OR (last_completion_status == 0 OR last_completion_status IS NULL OR task_recurrence_id IS NOT NULL) AND NOT isDraft) " +
                "AND (startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND (last_completion_status == 0 OR last_completion_status IS NULL OR task_recurrence_id IS NOT NULL) AND NOT :hideLateTasks " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY last_completion_status ASC, estimatedWorkingTime ASC, skillLevel DESC, rt.is_active DESC, priority ASC, urgency DESC, importance DESC"
    )
    fun getTasksOrderBy2minutesRules(
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * " +
                "FROM thing_to_do_view t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE (last_completion_status != :hideCompleted OR last_completion_status == 0 OR last_completion_status is NULL) AND NOT isDraft " +
                "AND (startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND (last_completion_status == 0 OR last_completion_status is NULL OR task_recurrence_id IS NOT NULL) AND NOT :hideLateTasks " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY last_completion_status ASC, task_due_date/8640000 ASC, estimatedWorkingTime DESC, estimatedEmotions ASC, priority DESC, importance DESC, deadline ASC, skillLevel ASC"
    )
    fun getTasksOrderByEatTheFrogSort(
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>>

    @Transaction
    @Query("SELECT * FROM thing_to_do_view t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE (last_completion_status != :hideCompleted OR last_completion_status == 0 OR last_completion_status is NULL) AND NOT isDraft " +
                "AND (startDate < :endOfDay AND (last_completion_status == 0 OR last_completion_status is NULL) AND is_active == 0 " +    //startDate BETWEEN :startOfDay AND :endOfDay
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND (last_completion_status == 0 OR last_completion_status is NULL OR task_recurrence_id IS NOT NULL) AND NOT :hideLateTasks " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY last_completion_status ASC, task_due_date/8640000 ASC, estimatedWorkingTime ASC, estimatedEmotions DESC, priority DESC, skillLevel ASC, urgency DESC, importance DESC, is_active ASC"
    )
    fun getTasksOrderByCreatorSort(
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id = :parentId " +
                "ORDER BY isCompleted DESC, priority ASC, estimatedWorkingTime ASC, skillLevel ASC, urgency DESC, importance DESC, is_active ASC"
    )
    fun getSubTasks(parentId: Long): Flow<List<Task>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "WITH LatestCompletion AS (" + // Sélectionne la dernière complétion pour chaque tâche
                "SELECT " +
                "c.parent_task_id, " +
                "MAX(completionDate) AS lastCompletionDate, " +
                "completionDate," +
                "isCompleted " +
                "FROM task_completion_table c " +
                "GROUP BY c.parent_task_id " +
                ") " +
                "SELECT * FROM task_table t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "LEFT JOIN LatestCompletion AS lc ON t.task_id = lc.parent_task_id " +
                "WHERE task_due_date < :todayDate AND is_active AND NOT isDraft ORDER BY task_due_date ASC"
    )
    suspend fun getMissedRecurringTasks(todayDate: Long): List<ThingToDo>


    // ***********  Statistics *********** //
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT * FROM thing_to_do_view ttd " +
                "LEFT JOIN task_recurrence_table tr ON ttd.task_recurrence_id = tr.recurrence_id " +
                "LEFT JOIN latest_completion_view lc ON ttd.task_id = lc.parent_task_id " +
                "WHERE last_completion_status AND (NOT is_active OR is_active is NULL) " +
                "ORDER BY last_completion_date DESC"
    )
    fun getCompletedTasks(): Flow<List<ThingToDo>>

    @Query("SELECT COUNT(task_id) FROM task_table t " +
                "LEFT JOIN latest_completion_view AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON (t.task_recurrence_id = rt.recurrence_id AND NOT rt.is_active) " +
                "WHERE is_completed OR (task_recurrence_id IS NOT NULL AND NOT is_active AND is_completed)"
    )
    suspend fun getCompletedTasksCount(): Int


    @Query(
                "SELECT COUNT(*) FROM task_table t " +
                "LEFT JOIN latest_completion_view c ON t.task_id = c.parent_task_id " +
                "WHERE is_completed AND category_id = :categoryId "
    )
    suspend fun getCompletedTasksCount(categoryId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id IS NULL AND isCompleted AND nature = 'PROJECT'"
    )
    suspend fun getCompletedProjectsCount(): Int

    @Query(
        "SELECT COUNT(*) FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id IS NULL AND isCompleted AND nature = 'PROJECT' AND category_id = :categoryId "
    )
    suspend fun getCategoryCompletedProjectsCount(categoryId: Long): Int

    @Transaction
    @Query(
        "SELECT COUNT(*) FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE isCompleted == 0 AND task_due_date > :endDate AND task_due_date < :endDateFilter"
    )
    fun getUpcomingTasksCountFilter(endDate: Long, endDateFilter: Long): Flow<Int>

    @Transaction
    @Query(
        "SELECT COUNT(*) FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE isCompleted == 0 AND task_due_date > :endDate"
    )
    fun getUpcomingTasksCount(endDate: Long): Flow<Int>

    // TODO: Create test to check number of returns
    @Query(
        "SELECT round(1.0 * COUNT(CASE WHEN is_completed THEN 1 END) / COUNT(*) * 100,1) FROM task_table t " +
                "LEFT JOIN latest_completion_view AS lc ON t.task_id = lc.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE NOT rt.is_active OR rt.is_active IS NULL"
    )
    suspend fun getAchievementRate(): Float

    @Query(
        "SELECT round(1.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*) * 100,1)  FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE category_id = :categoryId"
    )
    suspend fun getAchievementRateByCategory(categoryId: Long): Float

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id IS NULL AND nature = 'PROJECT'"
    )
    suspend fun getProjectsAchievementRate(): Float

    @Query(
        "SELECT round(CASE WHEN COUNT(*) != 0 THEN 100.0 * COUNT(isCompleted) / COUNT(*) END,1)  " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id IS NULL AND isCompleted AND nature = 'PROJECT' AND category_id = :categoryId"
    )
    suspend fun getProjectsAchievementRateByCategory(categoryId: Long): Float

    // TODO: remove "/86400000" with readable expression
    /**
     * Function to retrieve completed tasks count of a certain category by day in a period.
     *
     * CompletionDate/ 8640 000 (number of milliseconds in a day to not take in count time)
     */
    @Query(
        "SELECT completionDate, COUNT(*) as completedCount " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE category_id = :categoryId AND completionDate BETWEEN :startDate AND :endDate AND isCompleted " +
                "GROUP BY completionDate/ 86400000"
    )
    suspend fun getCompletedTasksByPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): List<TtdAchieved>

    /**
     * Function to retrieve all completed tasks count by day in a period.
     *
     * CompletionDate/ 8640 000 (number of milliseconds in a day to not take in count time)
     *
     * @return Flow<List<TtdAchieved>> with completion date day and count
     */
    @Query(
        "SELECT completionDate, COUNT(*) as completedCount " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE completionDate BETWEEN :startDate AND :endDate AND isCompleted " +
                "GROUP BY completionDate/ 86400000"
    )
    suspend fun getCompletedTasksByPeriod(startDate: Long, endDate: Long): List<TtdAchieved>

    @Query(
        "SELECT SUM(duration) FROM task_table t " +
                "LEFT JOIN worksession AS ws ON t.task_id = ws.parentTaskId " +
                "WHERE nature != 'PROJECT' "
    )
    suspend fun getTotalTimeWorked(): Long

    @Query(
        "SELECT SUM(duration) FROM task_table t " +
                "LEFT JOIN worksession AS ws ON t.task_id = ws.parentTaskId " +
                "WHERE category_id = :categoryId AND nature != 'PROJECT'"
    )
    suspend fun getTotalTimeWorked(categoryId: Long): Long

    @Query(
        "SELECT title, SUM(duration) as totalTimeWorked FROM task_table t " +
                "LEFT JOIN worksession AS ws ON t.task_id = ws.parentTaskId " +
                "WHERE category_id = :categoryId AND nature != 'PROJECT' " +
                "GROUP BY t.task_id"
    )
    suspend fun getTimeWorkedPerTask(categoryId: Long): List<TimeWorkedDistribution>

    /**
     * To retrieve sum time worked by tasks grouped by category (even if tasks haven't a category).
     */
    @Query(
        "SELECT category_title as title, SUM(duration) as totalTimeWorked " +
                "FROM task_table t " +
                "LEFT JOIN category c ON t.category_id = c.category_id " +
                "LEFT JOIN worksession AS ws ON t.task_id = ws.parentTaskId " +
                "WHERE nature != 'PROJECT'" +
                "GROUP BY t.category_id"
    )
    suspend fun getTimeWorkedPerCategory(): List<TimeWorkedDistribution>

    /**
     * Get accuracy rate of estimated work time for all tasks completed.
     * If the current working time is equal to estimated time +- errorPercent so the thingToDo's time work is well estimated.
     */
    @Query(
        """WITH task_work_time AS (
    -- Calcul du temps total passé sur chaque tâche
    SELECT 
        parentTaskId,
        SUM(duration) AS total_work_time
    FROM worksession
    GROUP BY parentTaskId
),
task_completion_count AS (
    -- Comptage du nombre de complétions par tâche (utile pour les tâches répétitives)
    SELECT 
        parent_task_id,
        COUNT(*) AS completion_count
    FROM task_completion_table
    GROUP BY parent_task_id
),
task_avg_work_time AS (
    -- Calcul du temps moyen passé par session pour les tâches répétitives
    SELECT 
        tc.parent_task_id,
        COALESCE(tw.total_work_time, 0) AS total_work_time,
        COALESCE(tc.completion_count, 1) AS completion_count,
        COALESCE(tw.total_work_time, 0) / COALESCE(tc.completion_count, 1) AS avg_work_time
    FROM task_work_time tw
    LEFT JOIN task_completion_count tc ON tw.parentTaskId = tc.parent_task_id
),
last_completion AS (
	SELECT 
		parent_task_id,
		MAX(completionDate) AS last_completion_date
	FROM task_completion_table
	GROUP BY parent_task_id
),
accuracy_check AS (
    -- Comparaison avec le temps estimé
    SELECT 
        tt.category_id,
        tt.estimatedWorkingTime,
        ta.avg_work_time,
        CASE 
            WHEN ta.avg_work_time BETWEEN tt.estimatedWorkingTime * (1- :errorPercent) AND tt.estimatedWorkingTime * (1 + :errorPercent)
            THEN 1 ELSE 0 
        END AS well_estimated,
		lc.last_completion_date
    FROM task_table tt
    LEFT JOIN task_avg_work_time ta ON tt.task_id = ta.parent_task_id
	LEFT JOIN last_completion lc ON lc.parent_task_id = tt.task_id
)
-- Calcul du taux de précision global
SELECT 
    COUNT(CASE WHEN well_estimated = 1 THEN 1 END) * 100.0 / COUNT(*) AS estimation_accuracy
FROM accuracy_check
WHERE estimatedWorkingTime IS NOT NULL
    """
    )
    suspend fun getAccuracyRateOfEstimatedWorkTime(errorPercent: Float): Float

    // TODO: Test this function to ensure constituency
    /**
     * Get accuracy rate of estimated work time for all tasks completed.
     * If the current working time is equal to estimated time +- errorPercent so the thingToDo's time work is well estimated.
     */
    @Query(
        """WITH task_work_time AS (
    -- Calcul du temps total passé sur chaque tâche
    SELECT 
        parentTaskId as task_id,
        SUM(duration) AS total_work_time
    FROM worksession
    GROUP BY task_id
),
task_completion_count AS (
    -- Comptage du nombre de complétions par tâche (utile pour les tâches répétitives)
    SELECT 
        parent_task_id,
        COUNT(*) AS completion_count
    FROM task_completion_table
    GROUP BY parent_task_id
),
task_avg_work_time AS (
    -- Calcul du temps moyen passé par session pour les tâches répétitives
    SELECT 
        tc.parent_task_id,
        COALESCE(tw.total_work_time, 0) AS total_work_time,
        COALESCE(tc.completion_count, 1) AS completion_count,
        COALESCE(tw.total_work_time, 0) / COALESCE(tc.completion_count, 1) AS avg_work_time
    FROM task_work_time tw
    LEFT JOIN task_completion_count tc ON tw.task_id = tc.parent_task_id
),
last_completion AS (
	SELECT 
		parent_task_id,
		MAX(completionDate) AS last_completion_date
	FROM task_completion_table
	GROUP BY parent_task_id
),
accuracy_check AS (
    -- Comparaison avec le temps estimé
    SELECT 
        tt.task_id,
        tt.category_id,
        tt.estimatedWorkingTime,
        ta.avg_work_time,
        CASE 
            WHEN ta.avg_work_time BETWEEN tt.estimatedWorkingTime * (1- :errorPercent) AND tt.estimatedWorkingTime * (1 + :errorPercent)
            THEN 1 ELSE 0 
        END AS well_estimated,
		lc.last_completion_date
    FROM task_table tt
    LEFT JOIN task_avg_work_time ta ON tt.task_id = ta.parent_task_id
	LEFT JOIN last_completion lc ON lc.parent_task_id = tt.task_id
)
-- Calcul du taux de précision global
SELECT 
    COUNT(CASE WHEN well_estimated = 1 THEN 1 END) * 100.0 / COUNT(*) AS estimation_accuracy
FROM accuracy_check
WHERE estimatedWorkingTime IS NOT NULL AND task_id = :taskId
    """
    )
    suspend fun getAccuracyRateOfEstimatedWorkTime(errorPercent: Float, taskId: Long): Float

    @Query(
        """WITH task_work_time AS (
    -- Calcul du temps total passé sur chaque tâche
    SELECT 
        parentTaskId,
        SUM(duration) AS total_work_time
    FROM worksession
    GROUP BY parentTaskId
),
task_completion_count AS (
    -- Comptage du nombre de complétions par tâche (utile pour les tâches répétitives)
    SELECT 
        parent_task_id,
        COUNT(*) AS completion_count
    FROM task_completion_table
    GROUP BY parent_task_id
),
task_avg_work_time AS (
    -- Calcul du temps moyen passé par session pour les tâches répétitives
    SELECT 
        tc.parent_task_id,
        COALESCE(tw.total_work_time, 0) AS total_work_time,
        COALESCE(tc.completion_count, 1) AS completion_count,
        COALESCE(tw.total_work_time, 0) / COALESCE(tc.completion_count, 1) AS avg_work_time
    FROM task_work_time tw
    LEFT JOIN task_completion_count tc ON tw.parentTaskId = tc.parent_task_id
),
last_completion AS (
	SELECT 
		parent_task_id,
		MAX(completionDate) AS last_completion_date
	FROM task_completion_table
	GROUP BY parent_task_id
),
accuracy_check AS (
    -- Comparaison avec le temps estimé
    SELECT 
        tt.category_id,
        tt.estimatedWorkingTime,
        ta.avg_work_time,
        CASE 
            WHEN ta.avg_work_time BETWEEN tt.estimatedWorkingTime * (1- :errorPercent) AND tt.estimatedWorkingTime * (1 + :errorPercent)
            THEN 1 ELSE 0 
        END AS well_estimated,
		lc.last_completion_date
    FROM task_table tt
    LEFT JOIN task_avg_work_time ta ON tt.task_id = ta.parent_task_id
	LEFT JOIN last_completion lc ON lc.parent_task_id = tt.task_id
),
accuracy_over_time AS (
    SELECT
        strftime('%Y-%m', last_completion_date / 1000, 'unixepoch') AS period,
        COUNT(CASE WHEN well_estimated = 1 THEN 1 END) * 100.0 / COUNT(*)  AS rate -- estimation_accuracy
    FROM accuracy_check
	WHERE estimatedWorkingTime IS NOT NULL AND last_completion_date BETWEEN :startDate AND :endDate
    GROUP BY period
)
-- Calcul du taux de précision par periode
SELECT * FROM accuracy_over_time
ORDER BY period ASC
    """
    )
    suspend fun getAccuracyRateOfEstimatedWorkTimeByPeriod(
        startDate: Long,
        endDate: Long,
        errorPercent: Float
    ): List<IndicatorRateByPeriod>

    @Query(
        """WITH task_work_time AS (
    -- Calcul du temps total passé sur chaque tâche
    SELECT 
        parentTaskId,
        SUM(duration) AS total_work_time
    FROM worksession
    GROUP BY parentTaskId
),
task_completion_count AS (
    -- Comptage du nombre de complétions par tâche (utile pour les tâches répétitives)
    SELECT 
        parent_task_id,
        COUNT(*) AS completion_count
    FROM task_completion_table
    GROUP BY parent_task_id
),
task_avg_work_time AS (
    -- Calcul du temps moyen passé par session pour les tâches répétitives
    SELECT 
        tc.parent_task_id,
        COALESCE(tw.total_work_time, 0) AS total_work_time,
        COALESCE(tc.completion_count, 1) AS completion_count,
        COALESCE(tw.total_work_time, 0) / COALESCE(tc.completion_count, 1) AS avg_work_time
    FROM task_work_time tw
    LEFT JOIN task_completion_count tc ON tw.parentTaskId = tc.parent_task_id
),
last_completion AS (
	SELECT 
		parent_task_id,
		MAX(completionDate) AS last_completion_date
	FROM task_completion_table
	GROUP BY parent_task_id
),
accuracy_check AS (
    -- Comparaison avec le temps estimé
    SELECT 
        tt.category_id,
        tt.estimatedWorkingTime,
        ta.avg_work_time,
        CASE 
            WHEN ta.avg_work_time BETWEEN tt.estimatedWorkingTime * (1- :errorPercent) AND tt.estimatedWorkingTime * (1 + :errorPercent)
            THEN 1 ELSE 0 
        END AS well_estimated,
		lc.last_completion_date
    FROM task_table tt
    LEFT JOIN task_avg_work_time ta ON tt.task_id = ta.parent_task_id
	LEFT JOIN last_completion lc ON lc.parent_task_id = tt.task_id
)
-- Calcul du taux de précision global
SELECT 
    COUNT(CASE WHEN well_estimated = 1 THEN 1 END) * 100.0 / COUNT(*) AS estimation_accuracy
FROM accuracy_check
WHERE estimatedWorkingTime IS NOT NULL AND category_id = :categoryId
    """
    )
    suspend fun getCategoryAccuracyRateOfEstimatedWorkTime(
        categoryId: Long,
        errorPercent: Float
    ): Float

    @Query(
        """WITH task_work_time AS (
    -- Calcul du temps total passé sur chaque tâche
    SELECT 
        parentTaskId,
        SUM(duration) AS total_work_time
    FROM worksession
    GROUP BY parentTaskId
),
task_completion_count AS (
    -- Comptage du nombre de complétions par tâche (utile pour les tâches répétitives)
    SELECT 
        parent_task_id,
        COUNT(*) AS completion_count
    FROM task_completion_table
    GROUP BY parent_task_id
),
task_avg_work_time AS (
    -- Calcul du temps moyen passé par session pour les tâches répétitives
    SELECT 
        tc.parent_task_id,
        COALESCE(tw.total_work_time, 0) AS total_work_time,
        COALESCE(tc.completion_count, 1) AS completion_count,
        COALESCE(tw.total_work_time, 0) / COALESCE(tc.completion_count, 1) AS avg_work_time
    FROM task_work_time tw
    LEFT JOIN task_completion_count tc ON tw.parentTaskId = tc.parent_task_id
),
last_completion AS (
	SELECT 
		parent_task_id,
		MAX(completionDate) AS last_completion_date
	FROM task_completion_table
	GROUP BY parent_task_id
),
accuracy_check AS (
    -- Comparaison avec le temps estimé
    SELECT 
        tt.category_id,
        tt.estimatedWorkingTime,
        ta.avg_work_time,
        CASE 
            WHEN ta.avg_work_time BETWEEN tt.estimatedWorkingTime * (1- :errorPercent) AND tt.estimatedWorkingTime * (1 + :errorPercent)
            THEN 1 ELSE 0 
        END AS well_estimated,
		lc.last_completion_date
    FROM task_table tt
    LEFT JOIN task_avg_work_time ta ON tt.task_id = ta.parent_task_id
	LEFT JOIN last_completion lc ON lc.parent_task_id = tt.task_id
),
accuracy_over_time AS (
    SELECT
        strftime('%Y-%m', last_completion_date / 1000, 'unixepoch') AS period,
        COUNT(CASE WHEN well_estimated = 1 THEN 1 END) * 100.0 / COUNT(*)  AS rate --estimation_accuracy
    FROM accuracy_check
	WHERE estimatedWorkingTime IS NOT NULL AND category_id = :categoryId AND last_completion_date BETWEEN :startDate AND :endDate
    GROUP BY period
)
-- Calcul du taux de précision par catégorie et par periode
SELECT * FROM accuracy_over_time
ORDER BY period ASC
    """
    )
    //COUNT(*) AS nb_tasks,
    suspend fun getCategoryAccuracyRateOfEstimatedWorkTimeByPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long,
        errorPercent: Float
    ): List<IndicatorRateByPeriod>

    @Query(
        """
        SELECT 
            round(100.0 * COUNT(CASE WHEN last_completion_date <= task_due_date THEN 1 END) / COUNT(t.task_id),1) as completion_on_time_rate
        FROM task_table t
        LEFT JOIN LATEST_COMPLETION_VIEW AS tc ON t.task_id = tc.parent_task_id
        WHERE is_completed
        """
    )
    suspend fun getOnTimeCompletionTasksRate(): Float

    // TODO: remove dividing by number and use integrate function
    /**
     * Get on time completion by week or by month
     **/
    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completionDate <= task_due_date THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted AND task_due_date BETWEEN :startDate AND :endDate " +
                "GROUP BY task_due_date /8640000"
    )
    suspend fun getOnTimeCompletionTasksRateByPeriod(startDate: Long, endDate: Long): List<Float>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completionDate <= task_due_date THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted AND category_id = :categoryId"
    )
    suspend fun getOnTimeCompletionCategoryTasksRate(categoryId: Long): Float?

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completionDate <= task_due_date THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted AND category_id = :categoryId AND task_due_date BETWEEN :startDate AND :endDate " +
                "GROUP BY task_due_date /8640000"
    )
    suspend fun getOnTimeCompletionTasksRateByCategoryAndPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): List<Float>

    @Query(
        """WITH ranked_completions AS (
        SELECT
            c.parent_task_id,
            completionDate,
            LAG(completionDate) OVER (PARTITION BY c.parent_task_id ORDER BY completionDate) AS prev_completion,
            CASE WHEN LAG(completionDate) OVER (PARTITION BY c.parent_task_id ORDER BY completionDate) IS NULL
                OR completionDate - LAG(completionDate) OVER (PARTITION BY c.parent_task_id ORDER BY completionDate) > 86400000
                THEN 1
                ELSE 0
            END AS new_streak
    FROM task_completion_table c
    LEFT JOIN task_table t ON t.task_id = c.parent_task_id
    LEFT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_id
    WHERE isCompleted = 1 AND is_active
    ),
    streak_groups AS (
    SELECT
    parent_task_id,
    completionDate,
    SUM(new_streak) AS streak_id
    FROM ranked_completions
    ),
    streak_counts AS (
    SELECT
    parent_task_id,
    streak_id,
    COUNT(*) AS streak_length
    FROM streak_groups
    GROUP BY parent_task_id, streak_id
    )
    SELECT
        t.title,
        MAX(streak_length) AS streak
    FROM streak_counts s 
    LEFT JOIN task_table t ON t.task_id = s.parent_task_id 
    GROUP BY t.task_id"""
    )
    suspend fun getTaskMaxStreak(): TtdStreakInfo

    @Query(
        """WITH ranked_completions AS (
    SELECT 
        c.parent_task_id,
        completionDate,
        LAG(completionDate) OVER(ORDER BY completionDate) AS prev_completion,
        CASE 
            WHEN LAG(completionDate) OVER (PARTITION BY c.parent_task_id ORDER BY completionDate) IS NULL 
                 OR completionDate - LAG(completionDate) OVER (PARTITION BY c.parent_task_id ORDER BY completionDate) > 86400000
            THEN 1
            ELSE 0
        END AS new_streak
    FROM task_completion_table c
	LEFT JOIN task_table t ON t.task_id = c.parent_task_id
	LEFT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_id
    WHERE isCompleted = 1 AND is_active
    ),
    streak_groups AS (
        SELECT 
            parent_task_id,
            completionDate,
            SUM(new_streak) OVER (PARTITION BY parent_task_id ORDER BY completionDate) AS streak_id
        FROM ranked_completions
    ),
    streak_counts AS (
        SELECT 
            parent_task_id,
            streak_id,
            COUNT(*) AS streak_length
        FROM streak_groups
        GROUP BY parent_task_id, streak_id
    )
    SELECT 
        t.title,
        MAX(streak_length) AS streak
    FROM streak_counts s
    LEFT JOIN task_table t ON t.task_id = s.parent_task_id
    WHERE category_id = :categoryId LIMIT 1"""
    )
    suspend fun getTaskMaxStreak(categoryId: Long): TtdStreakInfo

    @Query(
        """WITH completions_task AS (
            SELECT
                *
            FROM task_completion_table
            WHERE parent_task_id = :taskId
            ORDER BY completion_id
        ), 
        ordered AS (
            SELECT
                completion_id,
                isCompleted,
                -- cumul des "zéros"
                (SELECT COUNT(*) FROM completions_task c2 WHERE c2.completion_id <= c1.completion_id AND c2.isCompleted = 0) AS grp
            FROM completions_task c1
            ORDER BY completion_id
        ),
        only_ones AS (
            SELECT grp, COUNT(*) AS streak_length
            FROM ordered
            WHERE isCompleted = 1
            GROUP BY grp
        )
        SELECT MAX(streak_length) AS longest_streak
        FROM only_ones;
    """
    )
    suspend fun getLongestStreak(taskId: Long): Int

    // TODO: test to see if query return good value
    @Query(
        """WITH ranked_completions AS (
        SELECT 
            c.parent_task_id,
            completion_id,
            isCompleted,
            LAG(isCompleted) OVER (PARTITION BY c.parent_task_id ORDER BY completion_id) AS prev_status,
            CASE 
                WHEN LAG(isCompleted) OVER (PARTITION BY c.parent_task_id ORDER BY completion_id) == isCompleted THEN 1
                ELSE 0
            END AS new_streak
        FROM task_completion_table c
        LEFT JOIN task_table t ON t.task_id = c.parent_task_id
        LEFT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_recurrence_id
        WHERE t.task_recurrence_id IS NOT NULL
    ),
    streak_groups AS (
        SELECT 
            parent_task_id,
            completion_id,
            isCompleted,
            SUM(new_streak) OVER (PARTITION BY parent_task_id ORDER BY completion_id) AS streak_id
        FROM ranked_completions
    ),
    last_streak AS (
        SELECT 
            parent_task_id,
            MAX(completion_id) AS last_completion_id
        FROM streak_groups
        GROUP BY parent_task_id
    ),
    current_streak AS (
        SELECT 
            s.parent_task_id,
            s.isCompleted ,
            COUNT(*) AS streak_length
        FROM streak_groups s
        JOIN last_streak l ON s.parent_task_id = l.parent_task_id 
        WHERE s.streak_id = (SELECT streak_id FROM streak_groups WHERE completion_id = l.last_completion_id)
        GROUP BY s.parent_task_id, s.isCompleted
    )
    SELECT 
        title,
        MAX(streak_length) as streak
    FROM current_streak cs
    LEFT JOIN task_table t ON t.task_id = cs.parent_task_id
    """
    )
    suspend fun getTaskCurrentMaxStreak(): TtdStreakInfo

    @Query(
        """WITH ranked_completions AS (
        SELECT 
            c.parent_task_id,
            completion_id,
            isCompleted,
            LAG(isCompleted) OVER (PARTITION BY c.parent_task_id ORDER BY completion_id) AS prev_status,
            CASE 
                WHEN LAG(isCompleted) OVER (PARTITION BY c.parent_task_id ORDER BY completion_id) == isCompleted THEN 1
                ELSE 0
            END AS new_streak
        FROM task_completion_table c
        LEFT JOIN task_table t ON t.task_id = c.parent_task_id
        LEFT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_recurrence_id
        WHERE t.task_recurrence_id IS NOT NULL
    ),
    streak_groups AS (
        SELECT 
            parent_task_id,
            completion_id,
            isCompleted,
            SUM(new_streak) OVER (PARTITION BY parent_task_id ORDER BY completion_id) AS streak_id
        FROM ranked_completions
    ),
    last_streak AS (
        SELECT 
            parent_task_id,
            MAX(completion_id) AS last_completion_id
        FROM streak_groups
        GROUP BY parent_task_id
    ),
    current_streak AS (
        SELECT 
            s.parent_task_id,
            s.isCompleted AS current_status,
            COUNT(*) AS streak_length
        FROM streak_groups s
        JOIN last_streak l ON s.parent_task_id = l.parent_task_id 
        WHERE s.streak_id = (SELECT streak_id FROM streak_groups WHERE completion_id = l.last_completion_id)
        GROUP BY s.parent_task_id, s.isCompleted
    )
    SELECT 
        title,
        MAX(streak_length) as streak
    FROM current_streak cs
    LEFT JOIN task_table t ON t.task_id = cs.parent_task_id
    WHERE category_id = :categoryId
    """
    )
    suspend fun getTaskCurrentMaxStreak(categoryId: Long): TtdStreakInfo

    @Query(
        """
            WITH HabitsCompletionRate as (SELECT
                ROUND(
                    100.0 * SUM(CASE WHEN c.isCompleted = 1 THEN 1 ELSE 0 END) / COUNT(c.completion_id), 2
                ) AS completion_rate,
                c.parent_task_id as task_id
            FROM task_table t 
            LEFT JOIN task_completion_table c ON t.task_id = c.parent_task_id 
            LEFT JOIN task_recurrence_table tr ON t.task_recurrence_id = tr.recurrence_id 
            WHERE tr.is_active AND task_recurrence_id IS NOT NULL
            GROUP BY c.parent_task_id
            ) 
            SELECT 
                ROUND(SUM(completion_rate) / COUNT(task_id) * 100.0,2)
            FROM HabitsCompletionRate
                """
    )
    suspend fun getHabitCompletionRate(): Float

    @Query(
        "SELECT ROUND(100.0 * SUM(CASE WHEN c.isCompleted = 1 THEN 1 ELSE 0 END) / COUNT(t.task_id), 2) AS completion_rate " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table c ON t.task_id = c.parent_task_id " +
                "WHERE t.task_id = :taskId AND t.task_recurrence_id IS NOT NULL"
    )
    suspend fun getHabitCompletionRate(taskId: Long): Float

    @Query(    //t.title, SUM(CASE WHEN c.isCompleted = 1 THEN 1 ELSE 0 END) AS times_completed,
        "SELECT ROUND(100.0 * SUM(CASE WHEN c.isCompleted = 1 THEN 1 ELSE 0 END) / COUNT(t.task_id), 2) AS completion_rate " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table c ON t.task_id = c.parent_task_id " +
                "WHERE category_id = :categoryId AND t.task_recurrence_id IS NOT NULL"
    )
    suspend fun getCategoryHabitCompletionRate(categoryId: Long): Float

    /**
     * Get task missed times from last completion for repetitive task.
     */
    @Query(
        "WITH last_completion AS (" +
                "SELECT c.parent_task_id, " +
                " MAX(completionDate) AS last_completed_date " +
                "FROM task_completion_table c " +
                "LEFT JOIN task_table t ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_recurrence_id " +
                "WHERE isCompleted = 1 AND t.task_recurrence_id IS NOT NULL " +
                "GROUP BY c.parent_task_id" +
                ") " +
                "SELECT t.title, " +
                "strftime('%d-%m', datetime(ROUND(lc.last_completed_date / 1000), 'unixepoch')) as lastCompletionDate," +
                "COUNT(*) AS missedCount " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table c ON t.task_id = c.parent_task_id " +
                "JOIN task_recurrence_table tr ON t.task_recurrence_id = tr.recurrence_id " +
                "LEFT JOIN last_completion lc ON t.task_id = lc.parent_task_id " +
                "WHERE (c.completionDate > lc.last_completed_date OR lc.last_completed_date IS NULL) AND isCompleted = 0 AND task_id = :taskId " +
                "GROUP BY t.task_id;"
    )
    fun getMissedSinceLast(taskId: Long): Flow<CountSinceLastCompletion>

    // ************ Base Method *************** //

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Delete
    suspend fun deleteTasks(tasks: List<Task>)

    @Query("SELECT * FROM task_table WHERE task_id = :id LIMIT 1")
    suspend fun getTask(id: Long): Task

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM thing_to_do_view t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0 OR isCompleted IS NULL) " +
                "AND nature = 'PROJECT' " +
                "ORDER BY isCompleted ASC, task_due_date/8640000 ASC, estimatedWorkingTime ASC, priority DESC, skillLevel ASC, urgency DESC, importance DESC"
    )
    fun getProjects(hideCompleted: Boolean): Flow<List<ThingToDo>>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT t.* FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE (isCompleted == 0 OR isCompleted IS NULL  AND task_recurrence_id IS NULL)" +
                "AND task_id != :taskId"
    )
    fun getPotentialParentTasks(taskId: Long): Flow<List<Task>>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT t.* FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE (isCompleted == 0 OR isCompleted IS NULL AND task_recurrence_id IS NULL)"
    )
    fun getPotentialParentTasks(): Flow<List<Task>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM thing_to_do_view t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE is_active " +
                "ORDER BY estimatedWorkingTime ASC, priority ASC, skillLevel ASC, urgency DESC, importance DESC"
    )
    fun getRecurringTasks(): Flow<List<ThingToDo>>

    @Transaction
    @Query("SELECT * FROM task_table WHERE task_id =:parentTaskId LIMIT 1")
    suspend fun getParentTask(parentTaskId: Long): ThingToDo

    @Transaction
    @Query("SELECT * FROM thing_to_do_view WHERE isDraft")
    fun getDraftTask(): Flow<List<ThingToDo>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM thing_to_do_view t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted = 0 AND NOT (is_active OR is_active IS NULL)"
    )
    fun getTasksNotCompleted(): Flow<List<Task>>

    @Query("SELECT * FROM task_table")
    fun getAllTasks(): Flow<List<Task>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "SELECT tr.* FROM task_table t " +
                "LEFT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_recurrence_id " +
                "WHERE t.task_id = :taskId"
    )
    suspend fun getTaskRecurrenceInfos(taskId: Long): TaskRecurrenceWithDays?

    // ************ Test Methods *************** //
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        "WITH LatestCompletion AS (" + // Sélectionne la dernière complétion pour chaque tâche
                "SELECT " +
                "c.parent_task_id, " +
                "MAX(c.completionDate) AS lastCompletionDate, " +
                "c.isCompleted " +
                "FROM task_completion_table c " +
                "GROUP BY c.parent_task_id " +
                ") " +
                "SELECT t.* FROM task_table t " +
                "LEFT JOIN task_recurrence_table as r ON t.task_recurrence_id = r.recurrence_id " +
                "LEFT JOIN LatestCompletion as lc ON t.task_id = lc.parent_task_id"
    )
    fun getTasksRelations(): Flow<List<TaskRelations>>

    @Query(
        "SELECT * FROM task_table t " +
                "WHERE t.task_id = :id " +
                "LIMIT 1"
    )
    suspend fun getThingToDoDetails(id: Long): ThingToDoDetails

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "WITH RECURSIVE SubTasks AS (" +
                "SELECT t.* , 0 as depth, t.task_id as rootTtd, 1.0  AS weight " +
                "FROM task_table t " +
                "WHERE t.parent_task_id = :parentId " +
                "UNION ALL " +
                "SELECT t.*, depth + 1, st.rootTtd, weight /(SELECT count(*) FROM task_table WHERE parent_task_id = st.task_id) FROM task_table t " +
                "INNER JOIN SubTasks st ON t.parent_task_id = st.task_id " + // -- Ajoute récursivement les sous-tâches des sous-tâches
                "), " +
                "SubTasksInfos AS (" +
                "SELECT t.* , 0 as depth, t.task_id as rootTtd, 1.0  AS weight " +
                "FROM task_table t " +
                "INNER JOIN SubTasks st ON t.task_id = st.task_id " +
                "UNION ALL " +
                "SELECT t.*, depth + 1, st.rootTtd, weight /(SELECT count(*) FROM task_table WHERE parent_task_id = st.task_id) " +
                "FROM task_table t " +
                "JOIN SubTasksInfos st ON t.parent_task_id = st.task_id " +
                "), " +
                "LatestCompletion AS (" + // Sélectionne la dernière complétion pour chaque tâche
                "SELECT " +
                "c.parent_task_id, " +
                "MAX(c.completionDate) AS lastCompletionDate, " +
                "c.isCompleted " +
                "FROM task_completion_table c " +
                "GROUP BY c.parent_task_id " +
                ") " +
                "SELECT st.*, " +
                "COUNT(CASE WHEN st.depth = 1 THEN st.task_id END) AS total_main_sub_ttd, " + // Comptage des sous-tâches de niveau 1 "
                "COUNT(case when st.depth >= 1  and st.nature == 'SUB_TASK' then st.task_id end) AS nb_sub_tasks, " + // On enlève le projet principal du comptage
                "COUNT(case when lc.isCompleted = 1 and st.depth >= 1 and st.nature is not 'INTERMEDIATE_PROJECT' Then st.task_id end) AS nb_sub_tasks_completed, " +
                "lc.isCompleted as last_completion_status, " +
                "SUM((case when lc.isCompleted = 1 and st.depth >= 1 and st.nature is not 'INTERMEDIATE_PROJECT' Then lc.isCompleted * st.weight end) * 100.0 ) AS completion_rate " +
                "FROM SubTasks st " +
                "LEFT JOIN LatestCompletion AS lc ON st.task_id = lc.parent_task_id " +
                "GROUP BY st.rootTTd " +
                //"LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "ORDER BY last_completion_status, priority DESC, importance DESC, urgency DESC, estimatedWorkingTime DESC"
    )
    fun getSubThingTodos(parentId: Long): Flow<List<ThingToDo>>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        "WITH RECURSIVE SubTasks AS (" +
                "SELECT t.* , 0 as depth, t.task_id as rootTtd, 1.0  AS weight " +
                "FROM task_table t " +
                "WHERE t.task_id = :taskId " +
                "UNION ALL " +
                "SELECT t.*, depth + 1, st.rootTtd, weight /(SELECT count(*) FROM task_table WHERE parent_task_id = st.task_id) FROM task_table t " +
                "INNER JOIN SubTasks st ON t.parent_task_id = st.task_id " + // -- Ajoute récursivement les sous-tâches des sous-tâches
                ") " +
                "SELECT st.*, " +
                "COUNT(CASE WHEN st.depth = 1 THEN st.task_id END) AS total_main_sub_ttd, " + // Comptage des sous-tâches de niveau 1 "
                "COUNT(case when st.depth >= 1  and st.nature == 'SUB_TASK' then st.task_id end) AS nb_sub_tasks, " + // On enlève le projet principal du comptage
                "COUNT(case when lc.is_completed = 1 and st.depth >= 1 and st.nature is not 'INTERMEDIATE_PROJECT' Then st.task_id end) AS nb_sub_tasks_completed, " +
                "lc.is_completed as last_completion_status, " +
                "SUM((case when lc.is_completed = 1 and st.depth >= 1 and st.nature is not 'INTERMEDIATE_PROJECT' Then lc.is_completed * st.weight end) * 100.0 ) AS completion_rate " +
                "FROM SubTasks st " +
                "LEFT JOIN latest_completion_view AS lc ON st.task_id = lc.parent_task_id " +
                "GROUP BY st.rootTTd " +
                "LIMIT 1"
    )
    //"LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
    fun getThingToDo(taskId: Long): Flow<ThingToDo>

    @Query(
        "SELECT COUNT(*) as successCount FROM task_completion_table tc " +
                "WHERE tc.isCompleted AND tc.parent_task_id = :taskId " +
                "GROUP BY tc.parent_task_id"
    )
    suspend fun getHabitCompletionCount(taskId: Long): Int

    @Query(
        """WITH last_zero AS (
            SELECT completion_id
            FROM task_completion_table
            WHERE isCompleted = 0 AND parent_task_id = :taskId
            ORDER BY completion_id DESC
            LIMIT 1
        ),
        base AS (
            SELECT completion_id, isCompleted FROM task_completion_table WHERE parent_task_id = :taskId ORDER BY completion_id DESC
        )
        SELECT COUNT(*) AS current_streak
        FROM base
        WHERE isCompleted = 1
        AND completion_id > IFNULL((SELECT completion_id FROM last_zero), 0)
    """
    )
    suspend fun getCurrentStreakByTask(taskId: Long): Int

}