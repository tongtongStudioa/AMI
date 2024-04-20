package com.tongtongstudio.ami.ui.monitoring.projectStats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectStatsViewModel @Inject constructor(
    val repository: Repository,
    state: SavedStateHandle
) : ViewModel() {
    fun deleteSubtask(task: Ttd) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun onUndoClick(task: Ttd) = viewModelScope.launch {
        repository.insertTask(task.copy())
    }

    private val projectData = state.get<TaskWithSubTasks>("project")
    val subTasks = projectData?.subTasks ?: listOf<Ttd>()
    val workTime = projectData?.mainTask?.actualWorkTime ?: 0
    val estimatedTime = projectData?.mainTask?.estimatedTime
}