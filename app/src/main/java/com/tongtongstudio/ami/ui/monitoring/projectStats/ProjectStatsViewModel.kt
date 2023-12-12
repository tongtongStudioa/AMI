package com.tongtongstudio.ami.ui.monitoring.projectStats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProjectStatsViewModel @Inject constructor(
    val repository: Repository,
    state: SavedStateHandle
) : ViewModel() {
    private val projectData = state.get<TaskWithSubTasks>("project")
    val workTime = projectData?.mainTask?.actualWorkTime ?: 0
    val estimatedTime = projectData?.mainTask?.estimatedTime
}