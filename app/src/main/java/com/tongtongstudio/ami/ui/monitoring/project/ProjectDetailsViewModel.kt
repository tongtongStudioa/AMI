package com.tongtongstudio.ami.ui.monitoring.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    val repository: Repository,
    state: SavedStateHandle
) : ViewModel() {

    private val _uiState: MutableStateFlow<DetailsProjectUiState> = MutableStateFlow(
        DetailsProjectUiState()
    )
    val uiState: StateFlow<DetailsProjectUiState>
        get() = _uiState
    val projectId = state.get<Long>("project_id")

    init {
        viewModelScope.launch {
            repository.getThingToDo(projectId)?.collect { project ->
                _uiState.update {
                    it.copy(
                        projectId = project.taskRelations.mainTask.id,
                        project = project,
                        mainTask = project.taskRelations.mainTask
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSubThingToDo(projectId!!).collect { subTasks ->
                _uiState.update {
                    it.copy(
                        subTasks = subTasks,
                        workTime = repository.getProjectTimeWorked(projectId),
                        totalEstimatedWorkTime = repository.getTotalEstimatedWorkTime(projectId)
                    )
                }
            }
        }
    }
}