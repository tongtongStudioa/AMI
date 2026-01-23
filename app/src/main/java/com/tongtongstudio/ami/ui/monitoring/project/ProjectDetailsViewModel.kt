package com.tongtongstudio.ami.ui.monitoring.project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    val repository: Repository,
    state: SavedStateHandle
) : ViewModel() {

    // TODO: change to ui state implementation to show ui informations
    data class DetailsProjectUiState(
        val projectId: Long? = null,
        val project: Task? = null,
    )
    val projectId = state.get<Long>("project_id")
    val project = repository.getThingToDo(projectId)?.asLiveData()
    val subTasks = repository.getSubThingToDo(projectId!!).asLiveData()

    fun getProjectWorkTime(): Long = runBlocking{
        return@runBlocking repository.getProjectTimeWorked(projectId!!)
    }
}