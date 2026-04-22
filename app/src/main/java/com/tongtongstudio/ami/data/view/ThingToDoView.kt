package com.tongtongstudio.ami.data.view

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Status

@DatabaseView(
    value = "WITH RECURSIVE TaskH AS ( " +
            "SELECT t.* , 0 as depth, t.task_id as rootTtd, 1.0  AS weight " +
            "FROM task_table t " +
            "JOIN task_table st ON t.task_id = st.task_id " +
            "UNION ALL " +
            "SELECT t.*, depth + 1, th.rootTtd, weight /(SELECT count(*) FROM task_table WHERE parent_task_id = th.task_id) FROM task_table t " +
            "JOIN TaskH th ON t.parent_task_id = th.task_id " + //Ajoute récursivement les sous-tâches des sous-tâches
        ") " +
        "SELECT t.*, " +
            "COUNT(CASE WHEN st.depth = 1 THEN st.task_id END) AS total_main_sub_ttd, " + //Comptage des sous-tâches de niveau 1
    "COUNT(case when st.depth >= 1  and st.nature == 'SUB_TASK' then st.task_id end) AS nb_sub_tasks, " + //On enlève le projet principal du comptage
            "COUNT(case when lc.is_completed = 1 and st.depth >= 1 and st.nature is not 'INTERMEDIATE_PROJECT' Then st.task_id end) AS nb_sub_tasks_completed, " +
            "lc.is_completed as last_completion_status, " +
            "SUM((case when lc.is_completed = 1 and st.depth >= 1 and st.nature is not 'INTERMEDIATE_PROJECT' Then lc.is_completed * st.weight end) * 100.0 ) AS completion_rate " +
        "FROM task_table t " +
        "LEFT JOIN TaskH AS st ON st.task_id = t.task_id " +
        "LEFT JOIN LatestCompletionView AS lc ON t.task_id = lc.parent_task_id " +
        "GROUP by st.rootTtd"
)
data class ThingToDoView(
    val title: String,
    val priority: Int?,
    @ColumnInfo(name = "task_due_date")
    val dueDate: Long?,
    val startDate: Long? = null,
    val deadline: Long? = null,
    val description: String? = null,
    @ColumnInfo(defaultValue = "TASK")
    val nature: String = Nature.TASK.name,
    @ColumnInfo(defaultValue = "NOT_STARTED")
    val status: String = Status.NOT_STARTED.name,
    val importance: Int? = null,
    val urgency: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val isDraft: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val estimatedEmotions: Int = 1,
    val estimatedWorkingTime: Long? = null,
    val skillLevel: Int? = null,
    val creationDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "dependency_task_id", defaultValue = "NULL")
    val dependencyId: Long? = null,
    @ColumnInfo(name = "task_recurrence_id", defaultValue = "NULL")
    val recurrenceInfosId: Long? = null,
    @ColumnInfo(name = "category_id", defaultValue = "NULL")
    val categoryId: Long? = null,
    @ColumnInfo(name = "parent_task_id", defaultValue = "NULL")
    val parentTaskId: Long? = null,
    @ColumnInfo(name = "task_id")
    val id: Long = 0,
    @ColumnInfo(name = "total_main_sub_ttd")
    val totalMainSubTtd: Int,
    @ColumnInfo(name = "nb_sub_tasks")
    val nbSubTasks: Int,
    @ColumnInfo(name = "nb_sub_tasks_completed")
    val nbSubTasksCompleted: Int,
    @ColumnInfo(name = "last_completion_status")
    val lastCompletionStatus: Boolean,
    @ColumnInfo(name = "completion_rate")
    val completionRate: Float
)