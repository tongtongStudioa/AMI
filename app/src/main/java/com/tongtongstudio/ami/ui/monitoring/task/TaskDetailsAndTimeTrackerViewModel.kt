package com.tongtongstudio.ami.ui.monitoring.task


import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.TaskCompletion
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.WorkSession
import com.tongtongstudio.ami.timer.TimerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailsAndTimeTrackerViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle
) : ViewModel() {

    data class DetailUiState(
        val thingToDo: ThingToDo? = null,

        // Relations
        val taskCompletion: TaskCompletion? = null,
        val workSessions: List<WorkSession> = emptyList(),
        val currentTotalWorkTime: Long? = null,

        // Stats recurring task
        val successCount: Int? = null,
        val completionRate: Float? = null,
        val maxStreak: Int? = null,
        val currentStreak: Int? = null
    )

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    val taskId = state.get<Long>("task_id")!!

    init {
        viewModelScope.launch {
            repository.getThingToDo(taskId)?.collect { thingToDo->
                _uiState.update { it.copy(thingToDo = thingToDo) }
            }
        }

        viewModelScope.launch {
            repository.getLastTaskCompletion(taskId).collect { completion ->
                _uiState.update { it.copy(taskCompletion = completion) }
            }
        }

        viewModelScope.launch {
            repository.getWorkSessions(taskId).collect { sessions ->
                _uiState.update {
                    it.copy(
                        workSessions = sessions,
                        currentTotalWorkTime = sessions.sumOf { it.duration }
                    )
                }
            }
        }
        loadInitialData()
    }
    val taskCompletion: LiveData<TaskCompletion?> =
        taskId.let { repository.getLastTaskCompletion(it).asLiveData() }

    val currentTotalWorkTime: LiveData<Long?> =
        repository.getTaskTimeWorked(taskId).asLiveData()

    val workSessions: LiveData<List<WorkSession>> =
        taskId.let { repository.getWorkSessions(it).asLiveData() }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { uiState ->
                uiState.copy(
                    successCount = taskId.let { repository.getHabitCompletionCount(it) },
                    completionRate = taskId.let { repository.getHabitCompletionRate(it) },
                    maxStreak = taskId.let { repository.getMaxStreak(it) },
                    currentStreak = taskId.let { repository.getCurrentStreak(it) },
                    )
            }
        }
    }

    // External state
    val isServiceAlive: Boolean get() = state.get<Boolean>("is_service_alive") ?: false
    val timerType: TimerType get() = state.get<TimerType>("timer_type") ?: TimerType.STOPWATCH
    val isTracking: Boolean get() = state.get<Boolean>("is_tracking") ?: false
    val curTimeInMillis: Long get() = state.get<Long>("current_time") ?: 0L
    var fragmentPos: Int
        get() = state.get<Int>("fragment_pos") ?: 0
        set(value) = state.set("fragment_pos", value)

    /*var isServiceAlive: Boolean = false
    var timerType: TimerType = TimerType.STOPWATCH
    var curTimeInMillis: Long = 0L
    var isTracking = false

    var fragmentPos: Int = state.get<Int>("fragment_pos") ?: 0
        set(value) {
            field = value
            state["fragment_pos"] = value
        }*/

    // Pure functions for state updates
    fun updateServiceState(isAlive: Boolean) = updateState("is_service_alive", isAlive)

    fun updateTimerType(newType: TimerType) = updateState("timer_type", newType)

    fun updateTrackingState(isTracking: Boolean) = updateState("is_tracking", isTracking)

    fun updateCurrentTime(timeInMillis: Long) = updateState("current_time", timeInMillis)

    fun updateFragmentPosition(position: Int) = updateState("fragment_pos", position)

    fun saveTrackingTime(newWorkTimeSession: Long, comment: String? = null) =
        viewModelScope.launch {
            taskId.let { id ->
                repository.insertWorkSession(
                    WorkSession(
                        id,
                        newWorkTimeSession,
                        comment
                    )
                )
            }
        }

    fun updateTaskCompletionDate(newCompletionDate: Long) = viewModelScope.launch {
        taskCompletion.value?.let { currentCompletion ->
            repository.updateTaskCompletion(currentCompletion.copy(completionDate = newCompletionDate))
        }
    }

    fun updateWorkSession(workSession: WorkSession) = viewModelScope.launch {
        repository.updateWorkSession(workSession)
    }

    fun removeWorkSession(workSession: WorkSession) = viewModelScope.launch {
        repository.suppressWorkSession(workSession)
    }

    // Helper function for state management
    private fun <T> updateState(key: String, value: T) {
        state[key] = value
    }

    // Functional transformations for derived data
    /*fun getWorkSessionsWithComments(): LiveData<List<WorkSession>> =
        workSessions.map { sessions ->
            sessions.filter { it.comment?.isNotBlank() == true }
        }

    fun getTotalWorkTime(): LiveData<Long> =
        workSessions.map { sessions ->
            sessions.sumOf { it.duration }
        }
     */
}
