package com.tongtongstudio.ami.ui.dialog.recurring_task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.DaysOfWeek
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditRecurringTaskViewModel @Inject constructor(
    val repository: Repository
) : ViewModel() {

    data class EditRecurringTaskUiState(
        val frequency: String = Period.DAYS.name,
        val interval: Int = 1,
        val endDate: Long? = null,
        val occurrenceLimit: Int? = null,
        val daysOfWeek: List<DaysOfWeek>? = null,
        val recurrenceId: Long? = null,
        val startDate: Long? = null,
        val isLoading: Boolean = true,
        )

    private val _uiState = MutableStateFlow<EditRecurringTaskUiState>(EditRecurringTaskUiState())
    val uiState: StateFlow<EditRecurringTaskUiState> = _uiState

    fun updateDaysOfWeek(daysIds: List<Int>?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    daysOfWeek = if (daysIds != null)
                        repository.getDaysOfWeek(daysIds)
                    else null,
                )
            }
        }
    }

    fun updateInterval(interval: Int) {
        _uiState.update { it.copy(interval = interval) }
    }
    fun updateFrequency(listSelection: Int) {
        val newFrequency = when (listSelection) {
            0 -> Period.DAYS.name
            1 -> Period.WEEKS.name
            2 -> Period.MONTHS.name
            3 -> Period.YEARS.name
            else -> Period.DAYS.name
        }
        _uiState.update {
            it.copy(newFrequency)
        }
    }

    fun updateTaskRecurrenceWithDays(taskRecurrenceWithDays: TaskRecurrenceWithDays?) {
            _uiState.update { it.copy(
                recurrenceId = taskRecurrenceWithDays?.taskRecurrence?.recurrenceId
            ) }
    }

    fun buildTaskRecurrenceWithDays(state: EditRecurringTaskUiState): TaskRecurrenceWithDays {
        val taskRecurrence = TaskRecurrence(state.frequency, state.interval, state.startDate, state.endDate, occurrenceLimit = state.occurrenceLimit, recurrenceId = state.recurrenceId ?: 0L)
        val taskRecurrenceWithDays = TaskRecurrenceWithDays(taskRecurrence, state.daysOfWeek ?: emptyList())
        return taskRecurrenceWithDays
    }

}