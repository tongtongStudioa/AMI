package com.tongtongstudio.ami.ui.monitoring.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    val repository: Repository,
    state: SavedStateHandle
) : ViewModel() {
    fun deleteSubtask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun onUndoClick(task: Task) = viewModelScope.launch {
        repository.insertTask(task.copy())
    }

    private val projectData = state.get<ThingToDo>("project")
    val projectName = projectData?.mainTask?.title
    val description = projectData?.mainTask?.description
    val subTasks = projectData?.subTasks ?: listOf<Task>()
    val workTime = projectData?.mainTask?.currentWorkingTime ?: 0
    val estimatedTime = projectData?.mainTask?.estimatedWorkingTime
}