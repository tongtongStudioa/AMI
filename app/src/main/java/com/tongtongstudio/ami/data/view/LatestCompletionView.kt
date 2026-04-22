package com.tongtongstudio.ami.data.view

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    value = "SELECT " +
            "tc.parent_task_id, " +
           "tc.completionDate AS last_completion_date, " +
           "tc.isCompleted AS is_completed " +
        "FROM task_completion_table tc " +
        "INNER JOIN ( "+
            "SELECT parent_task_id, " +
                   "MAX(completionDate) AS max_date " +
            "FROM task_completion_table " +
            "GROUP BY parent_task_id " +
        ") latest " +
        "ON tc.parent_task_id = latest.parent_task_id "+
        "AND tc.completionDate = latest.max_date "
)
data class LatestCompletionView(
    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: Long,
    @ColumnInfo(name = "last_completion_date")
    val lastCompletionDate: Long,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean
)

const val LATEST_COMPLETION_VIEW = """
        SELECT tc.parent_task_id,
           tc.completionDate AS last_completion_date,
           tc.isCompleted AS is_completed
        FROM task_completion_table tc
        INNER JOIN (
            SELECT parent_task_id,
                   MAX(completionDate) AS max_date
            FROM task_completion_table
            GROUP BY parent_task_id
        ) latest
        ON tc.parent_task_id = latest.parent_task_id
        AND tc.completionDate = latest.max_date
        """