package com.tongtongstudio.ami.ui.monitoring.task


import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.WorkSession
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

    val currentTotalWorkTime: LiveData<Long?> = repository.getTaskTimeWorked(task!!.id).asLiveData()
    val workSessions = repository.getWorkSessions(task?.id!!).asLiveData()

    var curTimeInMillis: Long = 0L
    var isTracking = false

    fun saveTrackingTime(newWorkTimeSession: Long = 0L) = viewModelScope.launch {
        if (task == null) {
            return@launch
        }
        // TODO: add comment for the work session
        repository.insertWorkSession(WorkSession(task!!.id, newWorkTimeSession, null))
    }

    fun updateTaskCompletionDate(newCompletionDate: Long) = viewModelScope.launch {
        if (task != null) {
            val changeState = task!!.updateCheckedState(newCompletionDate = newCompletionDate)
            repository.updateTask(changeState)
            task = changeState
        }
    }


    fun removeWorkSession(workSession: WorkSession) = viewModelScope.launch {
        repository.suppressWorkSession(workSession)
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
