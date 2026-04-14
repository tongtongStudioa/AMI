package com.tongtongstudio.ami.ui.monitoring.project

import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo

data class DetailsProjectUiState(
    val projectId: Long? = null,
    val project: ThingToDo? = null,
    val mainTask: Task? = null,
    val subTasks: List<ThingToDo> = emptyList(),
    val workTime: Long = 0L,
    val totalEstimatedWorkTime: Long? = null
)