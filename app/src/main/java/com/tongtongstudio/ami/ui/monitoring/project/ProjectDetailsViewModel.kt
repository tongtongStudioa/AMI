package com.tongtongstudio.ami.ui.monitoring.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    val projectName = projectData?.taskRelations?.mainTask?.title
    val description = projectData?.taskRelations?.mainTask?.description
    val subTasks = repository.getSubTasks(projectData!!.taskRelations.mainTask.id).asLiveData()
    val estimatedTime = projectData?.taskRelations?.mainTask?.estimatedWorkingTime

    fun getProjectWorkTime(): Long = runBlocking{
        return@runBlocking repository.getProjectTimeWorked(projectData?.taskRelations?.mainTask?.id)
    }

    fun getProgressRatio(): Float {
        TODO("Not yet implemented")
        //val progress =
        // subTasks.sumOf { if (it.isCompleted && it.priority != null) it.priority else 0 }
        // val totalPriority = subTasks.sumOf { it.priority ?: 0 }
        // val progressPercentage =
        // progress / (if (subTasks.isEmpty() || totalPriority == 0) 1F else totalPriority
        // .toFloat()) * 100
        //return progressPercentage
    }
}