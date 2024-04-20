package com.tongtongstudio.ami.ui.monitoring.task


import androidx.lifecycle.*
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.timer.TimerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailsAndTimeTrackerViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle
) : ViewModel() {
    var fragmentPos: Int = state.get<Int>("fragment_pos") ?: 0
        set(value) {
            field = value
            state["fragment_pos"] = value
        }
    var isServiceAlive: Boolean = false
    var timerType: TimerType = TimerType.STOPWATCH
    var task = state.get<Ttd>("task")
        set(value) {
            field = value
            state["task"] = value
        }

    val name = task?.title
    val description = task?.description
    val startDate = task?.startDate
    val dueDate = task?.dueDate
    val deadline = task?.deadline
    val streak: Int = task?.currentStreak ?: 0

    val estimatedWorkingTime = task?.estimatedTime

    val primaryWorkTime = task?.actualWorkTime ?: 0
    private val _actualWorkTime = MutableLiveData<Long>(primaryWorkTime)
    val actualWorkTime: LiveData<Long>
        get() = _actualWorkTime

    var curTimeInMillis: Long = 0L
    var isTracking = false
    var timeWorked = 0L

    fun saveTrackingTime(newWorkTimeSession: Long = timeWorked) = viewModelScope.launch {
        if (task != null) {
            if (task!!.parentTaskId != null) {
                val parentTask = repository.getTask(task!!.parentTaskId!!)
                val updatedProjectWorkTime =
                    if (parentTask.actualWorkTime != null) parentTask.actualWorkTime + newWorkTimeSession else newWorkTimeSession
                repository.updateTask(
                    parentTask.copy(
                        actualWorkTime = updatedProjectWorkTime
                    )
                )
            }
            val updatedTaskTimeWorked =
                actualWorkTime.value?.plus(newWorkTimeSession) ?: newWorkTimeSession
            val updatedThingToDo = task!!.copy(actualWorkTime = updatedTaskTimeWorked)
            repository.updateTask(updatedThingToDo)
            _actualWorkTime.value = updatedTaskTimeWorked
        }
    }
}
