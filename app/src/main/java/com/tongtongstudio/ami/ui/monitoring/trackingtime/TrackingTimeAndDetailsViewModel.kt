package com.tongtongstudio.ami.ui.monitoring.trackingtime


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingTimeAndDetailsViewModel @Inject constructor(
    private val repository: Repository,
    state: SavedStateHandle
) : ViewModel() {

    val thingToDo = state.get<Task>("task")

    val nbCompleted: Int? = if (thingToDo is Task && thingToDo.isRecurring) {
        thingToDo.nbCompleted
    } else null

    val streak: Int? = if (thingToDo is Task && thingToDo.isRecurring) {
        thingToDo.streak
    } else null

    //private val _isTimerRunning = MutableLiveData<Boolean>(false)
    var isTimerRunning: Boolean = false

    //private val _timerCount = MutableLiveData<Long>(thingToDo?.getWorkTime() ?: 0)
    var timerCount: Long = thingToDo?.getWorkTime() ?: 0

    fun saveTrackingTime(newWorkTime: Long) = viewModelScope.launch {
        if (thingToDo != null) {
            if (thingToDo.projectId != null) {
                val project = repository.getProjectData(thingToDo.projectId).project
                val timeDifference = newWorkTime - timerCount
                val updatedProjectWorkTime =
                    if (project.pjtWorkTime != null) project.pjtWorkTime + timeDifference else newWorkTime
                repository.updateProject(
                    project.copy(
                        pjtWorkTime = updatedProjectWorkTime
                    )
                )
            }
            val updatedThingToDo = thingToDo.copy(taskWorkTime = newWorkTime)
            repository.updateTask(updatedThingToDo)
        }
        timerCount = newWorkTime
    }

    fun retrieveEstimatedTime(): Long? {

        return thingToDo?.getEstimatedTime()
    }
}
