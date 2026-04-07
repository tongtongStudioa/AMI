package com.tongtongstudio.ami.data.view

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "latest_completion_view",
    value = LATEST_COMPLETION_VIEW
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
        SELECT
            parent_task_id,
            MAX(completionDate) as last_completion_date,
            isCompleted as is_completed 
        FROM task_completion_table
        GROUP BY parent_task_id
    """