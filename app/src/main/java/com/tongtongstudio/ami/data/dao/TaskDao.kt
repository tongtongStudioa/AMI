package com.tongtongstudio.ami.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.CountSinceLastCompletion
import com.tongtongstudio.ami.data.datatables.IndicatorRateByPeriod
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
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
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table c ON c.parent_task_id = t.task_id " +
                "WHERE c.isCompleted == 0 AND NOT isDraft " +
                "AND (task_due_date > :endOfDay OR startDate > :endOfDay) " +
                "ORDER BY task_due_date/8640000 ASC, deadline/8640000 ASC, startDate ASC, priority DESC, estimatedWorkingTime DESC"
    )
    fun getLaterTasks(endOfDay: Long): Flow<List<ThingToDo>>

    @Transaction
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table c ON c.parent_task_id = t.task_id " +
                "WHERE c.isCompleted == 0 AND NOT isDraft AND (task_due_date BETWEEN :endOfDay AND :endOfDayFilter OR startDate BETWEEN :endOfDay AND :endOfDayFilter) " +
                "ORDER BY task_due_date/8640000 ASC, deadline/8640000 ASC, startDate ASC, priority DESC, estimatedWorkingTime DESC"
    )
    fun getLaterTasksFilter(endOfDay: Long, endOfDayFilter: Long): Flow<List<ThingToDo>>

    @Transaction
    @Query(
        "SELECT * FROM task_table AS t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0) AND NOT isDraft " +
                "AND (startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND isCompleted == 0 AND NOT :hideLateTasks " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY isCompleted, priority DESC, importance DESC, urgency DESC, estimatedWorkingTime DESC"
    )
    fun getTasksOrderByEisenhowerMatrixSort(
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>>

    @Transaction
    @Query(
        "SELECT * FROM task_table AS t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0 AND NOT isDraft) " +
                "AND (startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND isCompleted == 0 AND NOT :hideLateTasks " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY c.isCompleted ASC, estimatedWorkingTime ASC, skillLevel DESC, rt.is_active DESC, priority ASC, urgency DESC, importance DESC"
    )
    fun getTasksOrderBy2minutesRules(
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>>

    @Transaction
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0 AND NOT isDraft) " +
                "AND (startDate BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND isCompleted == 0 AND NOT :hideLateTasks " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY isCompleted ASC, task_due_date/8640000 ASC, estimatedWorkingTime DESC, priority DESC, importance DESC, deadline ASC, skillLevel ASC"
    )
    fun getTasksOrderByEatTheFrogSort(
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>>

    @Transaction
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0 AND NOT isDraft) " +
                "AND (startDate < :endOfDay AND isCompleted == 0 AND is_active == 0 " +    //startDate BETWEEN :startOfDay AND :endOfDay
                "OR task_due_date BETWEEN :startOfDay AND :endOfDay " +
                "OR task_due_date < :endOfDay AND isCompleted == 0 AND NOT :hideLateTasks " +
                "OR deadline BETWEEN :startOfDay AND :endOfDay) " +
                "ORDER BY isCompleted ASC, task_due_date/8640000 ASC, estimatedWorkingTime ASC, priority DESC, skillLevel ASC, urgency DESC, importance DESC, is_active ASC"
    )
    fun getTasksOrderByCreatorSort(
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<ThingToDo>>

    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id = :parentId " +
                "ORDER BY isCompleted DESC, priority ASC, estimatedWorkingTime ASC, skillLevel ASC, urgency DESC, importance DESC, is_active ASC"
    )
    fun getSubTasks(parentId: Long): Flow<List<Task>>

    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE task_due_date < :todayDate AND is_active AND NOT isDraft ORDER BY task_due_date ASC"
    )
    suspend fun getMissedRecurringTasks(todayDate: Long): List<ThingToDo>


    // ***********  Statistics *********** //
    @Transaction
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE isCompleted AND NOT is_active ORDER BY completionDate DESC"
    )
    fun getCompletedTasks(): Flow<List<ThingToDo>>

    @Query(
        "SELECT COUNT(*) FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE isCompleted AND NOT is_active" +
                ""
    )
    fun getCompletedTasksCount(): Flow<Int>


    @Query("SELECT COUNT(*) FROM task_table WHERE category_id = :categoryId ")
    fun getCategoryCompletedTasksCount(categoryId: Long): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id IS NULL AND isCompleted AND type = 'PROJECT'"
    )
    fun getCompletedProjectsCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id IS NULL AND isCompleted AND type = 'PROJECT' AND category_id = :categoryId "
    )
    fun getCategoryCompletedProjectsCount(categoryId: Long): Flow<Int>

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
        "SELECT round(1.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*) * 100,1) FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id "
    )
    fun getAchievementRate(): Flow<Float>

    @Query(
        "SELECT round(1.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*) * 100,1)  FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE category_id = :categoryId"
    )
    fun getAchievementRateByCategory(categoryId: Long): Flow<Float>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN isCompleted THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id IS NULL AND type = 'PROJECT'"
    )
    fun getProjectsAchievementRate(): Flow<Float>

    @Query(
        "SELECT round(CASE WHEN COUNT(*) != 0 THEN 100.0 * COUNT(isCompleted) / COUNT(*) END,1)  " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE t.parent_task_id IS NULL AND isCompleted AND type = 'PROJECT' AND category_id = :categoryId"
    )
    fun getProjectsAchievementRateByCategory(categoryId: Long): Flow<Float>

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
    fun getCompletedTasksByPeriodAndCategory(
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
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS c ON t.task_id = c.parent_task_id " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE completionDate BETWEEN :startDate AND :endDate AND isCompleted " +
                "GROUP BY completionDate/ 86400000"
    )
    fun getCompletedTasksByPeriod(startDate: Long, endDate: Long): Flow<List<TtdAchieved?>?>

    @Query(
        "SELECT SUM(duration) FROM task_table t " +
                "LEFT JOIN worksession AS ws ON t.task_id = ws.parentTaskId " +
                "WHERE type != 'PROJECT' "
    )
    fun getTotalTimeWorked(): Flow<Long>

    @Query(
        "SELECT SUM(duration) FROM task_table t " +
                "LEFT JOIN worksession AS ws ON t.task_id = ws.parentTaskId " +
                "WHERE category_id = :categoryId AND type != 'PROJECT'"
    )
    fun getSumCategoryTimeWorked(categoryId: Long): Flow<Long>

    @Query(
        "SELECT title, SUM(duration) as totalTimeWorked FROM task_table t " +
                "LEFT JOIN worksession AS ws ON t.task_id = ws.parentTaskId " +
                "WHERE category_id = :categoryId AND type != 'PROJECT' " +
                "GROUP BY t.task_id"
    )
    fun getTimeWorkedPerTask(categoryId: Long): Flow<List<TimeWorkedDistribution>>

    /**
     * To retrieve sum time worked by tasks grouped by category (even if tasks haven't a category).
     */
    @Query(
        "SELECT category_title as title, SUM(duration) as totalTimeWorked " +
                "FROM task_table t " +
                "LEFT JOIN category c ON t.category_id = c.category_id " +
                "LEFT JOIN worksession AS ws ON t.task_id = ws.parentTaskId " +
                "WHERE type != 'PROJECT'" +
                "GROUP BY t.category_id"
    )
    fun getTimeWorkedPerCategory(): Flow<List<TimeWorkedDistribution>>

    // TODO: Test this function to ensure constituency
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
    fun getAccuracyRateOfEstimatedWorkTime(errorPercent: Float): Flow<Float?>

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
    fun getAccuracyRateOfEstimatedWorkTimeByPeriod(
        startDate: Long,
        endDate: Long,
        errorPercent: Float
    ): Flow<List<IndicatorRateByPeriod>>

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
    fun getCategoryAccuracyRateOfEstimatedWorkTime(
        categoryId: Long,
        errorPercent: Float
    ): Flow<Float?>

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
    fun getCategoryAccuracyRateOfEstimatedWorkTimeByPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long,
        errorPercent: Float
    ): Flow<List<IndicatorRateByPeriod>>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completionDate <= task_due_date THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted"
    )
    fun getOnTimeCompletionTasksRate(): Flow<Float?>

    // TODO: get on time completion by week or by month
    // TODO: remove dividing by number and use integrate function
    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completionDate <= task_due_date THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted AND task_due_date BETWEEN :startDate AND :endDate " +
                "GROUP BY task_due_date /8640000"
    )
    fun getOnTimeCompletionTasksRateByPeriod(startDate: Long, endDate: Long): Flow<List<Float?>>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completionDate <= task_due_date THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted AND category_id = :categoryId"
    )
    fun getOnTimeCompletionCategoryTasksRate(categoryId: Long): Flow<Float?>

    @Query(
        "SELECT round(100.0 * COUNT(CASE WHEN completionDate <= task_due_date THEN 1 END) / COUNT(*),1) " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted AND category_id = :categoryId AND task_due_date BETWEEN :startDate AND :endDate " +
                "GROUP BY task_due_date /8640000"
    )
    fun getOnTimeCompletionTasksRateByCategoryAndPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<Float>>

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
    fun getMaxStreakTask(): Flow<TtdStreakInfo>

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
    fun getMaxStreakCategoryTask(categoryId: Long): Flow<TtdStreakInfo>

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
        RIGHT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_id
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
    """)
    fun getCurrentMaxStreakTask(): Flow<TtdStreakInfo>

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
        RIGHT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_id
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
    """)
    fun getCurrentMaxStreakCategoryTask(categoryId: Long): Flow<TtdStreakInfo>

    @Query(
        "SELECT ROUND(100.0 * SUM(CASE WHEN c.isCompleted = 1 THEN 1 ELSE 0 END) / COUNT(t.task_id), 2) AS completion_rate " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table c ON t.task_id = c.parent_task_id " +
                "RIGHT JOIN task_recurrence_table tr ON t.task_recurrence_id = tr.recurrence_id "
    )
    fun getHabitCompletionRate(): Flow<Float?>

    @Query(    //t.title, SUM(CASE WHEN c.isCompleted = 1 THEN 1 ELSE 0 END) AS times_completed,
        "SELECT ROUND(100.0 * SUM(CASE WHEN c.isCompleted = 1 THEN 1 ELSE 0 END) / COUNT(t.task_id), 2) AS completion_rate " +
                "FROM task_table t " +
                "LEFT JOIN task_completion_table c ON t.task_id = c.parent_task_id " +
                "RIGHT JOIN task_recurrence_table tr ON t.task_recurrence_id = tr.recurrence_id " +
                "WHERE category_id = :categoryId "
    )
    fun getCategoryHabitCompletionRate(categoryId: Long): Flow<Float?>

    /**
     * Get task missed times from last completion for repetitive task.
     */
    @Query(
        "WITH last_completion AS (" +
                "SELECT c.parent_task_id, " +
                " MAX(completionDate) AS last_completed_date " +
                "FROM task_completion_table c " +
                "LEFT JOIN task_table t ON t.task_id = c.parent_task_id " +
                "RIGHT JOIN task_recurrence_table tr ON tr.recurrence_id = t.task_id " +
                "WHERE isCompleted = 1 " +
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
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE (isCompleted != :hideCompleted OR isCompleted == 0) " +
                "AND t.parent_task_id IS NULL " +
                "AND type = 'PROJECT' " +
                "ORDER BY isCompleted ASC, task_due_date/8640000 ASC, estimatedWorkingTime ASC, priority DESC, skillLevel ASC, urgency DESC, importance DESC"
    )
    fun getProjects(hideCompleted: Boolean): Flow<List<ThingToDo>>

    // TODO: find nice way to show potential project
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted == 0"
    )
    fun getPotentialProject(): Flow<List<Task>>

    @Transaction
    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "WHERE is_active " +
                "ORDER BY estimatedWorkingTime ASC, priority ASC, skillLevel ASC, urgency DESC, importance DESC"
    )
    fun getRecurringTasks(): Flow<List<ThingToDo>>

    @Transaction
    @Query("SELECT * FROM task_table WHERE task_id =:parentTaskId LIMIT 1")
    suspend fun getParentTask(parentTaskId: Long): ThingToDo

    @Transaction
    @Query(
        "SELECT * FROM task_table " +
                "WHERE isDraft"
    )
    fun getDraftTask(): Flow<List<ThingToDo>>

    @Query(
        "SELECT * FROM task_table t " +
                "LEFT JOIN task_recurrence_table AS rt ON t.task_recurrence_id = rt.recurrence_id " +
                "LEFT JOIN task_completion_table AS tc ON t.task_id = tc.parent_task_id " +
                "WHERE isCompleted = 0 AND NOT (is_active OR is_active IS NULL)"
    )
    fun getTasksNotCompleted(): Flow<List<Task>>

    @Query("SELECT * FROM task_table")
    fun getAllTasks() : Flow<List<Task>>
}