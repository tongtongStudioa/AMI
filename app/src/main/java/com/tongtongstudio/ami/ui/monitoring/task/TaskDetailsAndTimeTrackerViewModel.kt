package com.tongtongstudio.ami.ui.monitoring.task


import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.WorkSession
import com.tongtongstudio.ami.timer.TimerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.w3c.dom.Comment
import javax.inject.Inject

@HiltViewModel
class TaskDetailsAndTimeTrackerViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle
) : ViewModel() {

    var isServiceAlive: Boolean = false
    var timerType: TimerType = TimerType.STOPWATCH
    var thingToDo = state.get<ThingToDo>("thingToDo")
        set(value) {
            field = value
            state["thingToDo"] = value
        }

    val name = thingToDo?.mainTask?.title
    val description = thingToDo?.mainTask?.description
    val startDate = thingToDo?.mainTask?.startDate
    val dueDate = thingToDo?.mainTask?.dueDate
    val deadline = thingToDo?.mainTask?.deadline
    val estimatedWorkingTime = thingToDo?.mainTask?.estimatedWorkingTime

    val currentTotalWorkTime: LiveData<Long?> = repository.getTaskTimeWorked(thingToDo!!.mainTask.id).asLiveData()
    val workSessions = repository.getWorkSessions(thingToDo?.mainTask?.id!!).asLiveData()

    val successCount: Int = repository.getTaskMaxStreak(thingToDo?.mainTask?.id!!)


    var curTimeInMillis: Long = 0L
    var isTracking = false

    fun saveTrackingTime(newWorkTimeSession: Long = 0L, date: Long? = null, comment: String? = null) = viewModelScope.launch {
        if (thingToDo == null) {
            return@launch
        }
        var workSession = WorkSession(thingToDo!!.mainTask.id, newWorkTimeSession, comment)
        if (date != null)
            workSession = workSession.copy(date = date)
        repository.insertWorkSession(workSession)
    }

    fun updateTaskCompletionDate(newCompletionDate: Long) = viewModelScope.launch {
        if (thingToDo != null) {
            val changeState = thingToDo!!.updateCheckedState(newCompletionDate = newCompletionDate)
            repository.updateTask(changeState)
            thingToDo = changeState
        }
    }


    fun removeWorkSession(workSession: WorkSession) = viewModelScope.launch {
        repository.suppressWorkSession(workSession)
    }

    val category: Category? = thingToDo?.category

    var fragmentPos: Int = state.get<Int>("fragment_pos") ?: 0
        set(value) {
            field = value
            state["fragment_pos"] = value
        }

}
