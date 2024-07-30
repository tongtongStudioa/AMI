package com.tongtongstudio.ami.ui.monitoring.task


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.timer.TimerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class TaskDetailsAndTimeTrackerViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle
) : ViewModel() {

    var isServiceAlive: Boolean = false
    var timerType: TimerType = TimerType.STOPWATCH
    var task = state.get<Task>("task")
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

    val estimatedWorkingTime = task?.estimatedWorkingTime

    val primaryWorkTime = task?.currentWorkingTime
    private val _actualWorkTime = MutableLiveData<Long?>(primaryWorkTime)
    val actualWorkTime: LiveData<Long?>
        get() = _actualWorkTime

    var curTimeInMillis: Long = 0L
    var isTracking = false
    var timeWorked = 0L

    // TODO: create another entity to save work session apart
    fun saveTrackingTime(newWorkTimeSession: Long = timeWorked) = viewModelScope.launch {
        if (task != null) {
            if (task!!.parentTaskId != null) {
                val parentTask = repository.getTask(task!!.parentTaskId!!)
                val updatedProjectWorkTime =
                    if (parentTask.currentWorkingTime != null) parentTask.currentWorkingTime + newWorkTimeSession else newWorkTimeSession
                repository.updateTask(
                    parentTask.copy(
                        currentWorkingTime = updatedProjectWorkTime
                    )
                )
            }
            val updatedTaskTimeWorked =
                actualWorkTime.value?.plus(newWorkTimeSession) ?: newWorkTimeSession
            val updatedThingToDo = task!!.copy(currentWorkingTime = updatedTaskTimeWorked)
            repository.updateTask(updatedThingToDo)
            _actualWorkTime.value = updatedTaskTimeWorked
        }
    }

    val category: String = runBlocking {
        return@runBlocking task?.categoryId?.let {
            repository.getCategoryById(
                it
            ).title
        } ?: ""
    }
    var fragmentPos: Int = state.get<Int>("fragment_pos") ?: 0
        set(value) {
            field = value
            state["fragment_pos"] = value
        }

}
