package com.tongtongstudio.ami.ui.monitoring.trackingtime


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Ttd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingTimeAndDetailsViewModel @Inject constructor(
    private val repository: Repository,
    state: SavedStateHandle
) : ViewModel() {

    val task = state.get<Ttd>("task")

    val successCount: Int? = if (task != null && task.isRecurring) {
        task.successCount
    } else null

    val streak: Int? = if (task != null && task.isRecurring) {
        task.currentStreak
    } else null

    //private val _isTimerRunning = MutableLiveData<Boolean>(false)
    var isTimerRunning: Boolean = false

    //private val _timerCount = MutableLiveData<Long>(thingToDo?.getWorkTime() ?: 0)
    var timerCount: Long = task?.actualWorkTime ?: 0

    // TODO: decompose this method and move it
    fun saveTrackingTime(newWorkTime: Long) = viewModelScope.launch {
        if (task != null) {
            if (task.parentTaskId != null) {
                val parentTask = repository.getTask(task.parentTaskId)
                val timeDifference = newWorkTime - timerCount
                val updatedProjectWorkTime =
                    if (parentTask.actualWorkTime != null) parentTask.actualWorkTime + timeDifference else newWorkTime
                repository.updateTask(
                    parentTask.copy(
                        actualWorkTime = updatedProjectWorkTime
                    )
                )
            }
            val updatedThingToDo = task.copy(actualWorkTime = newWorkTime)
            repository.updateTask(updatedThingToDo)
        }
        timerCount = newWorkTime
    }

    fun retrieveEstimatedTime(): Long? {
        return task?.estimatedTime
    }
}
