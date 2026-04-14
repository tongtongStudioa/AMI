package com.tongtongstudio.ami.ui.monitoring.task

import com.tongtongstudio.ami.data.datatables.TaskCompletion
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.WorkSession

data class DetailUiState(
    val thingToDo: ThingToDo? = null,

    // Relations
    val taskCompletion: TaskCompletion? = null,
    val workSessions: List<WorkSession> = emptyList(),
    val currentTotalWorkTime: Long? = null,

    val workSessionsCount: Int = 0,
    val meanWorkSessionDuration: Double =.0,
    val priorityIndex: Int? = null,
    val effectiveStartDate: Long? = null,
    // Stats recurring task
    val successCount: Int? = null,
    val completionRate: Float? = null,
    val maxStreak: Int? = null,
    val currentStreak: Int? = null
)