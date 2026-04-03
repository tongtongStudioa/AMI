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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
        val workTime: Long = 0L,
        val totalEstimatedWorkTime: Long? = null
    )

    val projectId = state.get<Long>("project_id")
    val project = repository.getThingToDo(projectId)?.asLiveData()
    val subTasks = repository.getSubThingToDo(projectId!!).asLiveData()

    private val _uiState: MutableStateFlow<DetailsProjectUiState> = MutableStateFlow(
        DetailsProjectUiState()
    )
    val uiState: StateFlow<DetailsProjectUiState>
        get() = _uiState

    init {
        getProjectWorkTime()
        getEstimatedWorkTime()
    }

    private fun getProjectWorkTime() = viewModelScope.launch(Dispatchers.IO) {
        _uiState.update {
            it.copy(
                workTime = repository.getProjectTimeWorked(projectId!!)
            )
        }
    }

    private fun getEstimatedWorkTime() = viewModelScope.launch(Dispatchers.IO) {
        _uiState.update {
            it.copy(totalEstimatedWorkTime = projectId?.let { parentId ->
                repository.getTotalEstimatedWorkTime(parentId)
            })
        }
    }
}