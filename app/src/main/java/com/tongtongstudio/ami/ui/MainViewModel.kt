package com.tongtongstudio.ami.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.*
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val mainEventChannel = Channel<SharedEvent>()
    val mainEvent = mainEventChannel.receiveAsFlow()

    val globalPreferencesFlow = preferencesManager.globalPreferencesFlow

    val currentLayoutMode = globalPreferencesFlow.asLiveData()

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    fun onLaterFilterSelected(laterFilter: LaterFilter) = viewModelScope.launch {
        preferencesManager.updateLaterFilter(laterFilter)
    }

    fun onLayoutModeSelected(layoutMode: LayoutMode) = viewModelScope.launch {
        preferencesManager.updateLayoutMode(layoutMode)
    }

    fun onCheckBoxChanged(thingToDo: Ttd, checked: Boolean) = viewModelScope.launch {
        val updatedTask = thingToDo.updateCheckedState(checked)
        repository.updateTask(updatedTask)
        // update advancement project after adapt task
        if (updatedTask.parentTaskId != null) {
            updateParentTask(updatedTask.parentTaskId)
        }
    }

    fun updateParentTask(parentTaskId: Long?) = viewModelScope.launch {
        if (parentTaskId != null) {
            val taskWithSubTasks: TaskWithSubTasks = repository.getComposedTask(parentTaskId)
            val isCompleted =
                taskWithSubTasks.getNbSubTasks() == taskWithSubTasks.getNbSubTasksCompleted()
            val updatedParentTask = taskWithSubTasks.mainTask.updateCheckedState(isCompleted)
            repository.updateTask(updatedParentTask)
        }
    }

    fun deleteTask(thingToDo: TaskWithSubTasks) = viewModelScope.launch {
        repository.deleteTask(thingToDo.mainTask)
        if (thingToDo.mainTask.parentTaskId != null) {
            updateParentTask(thingToDo.mainTask.parentTaskId)
        }
        mainEventChannel.send(
            SharedEvent.ShowUndoDeleteTaskMessage(thingToDo.mainTask)
        )
    }

    fun updateTask(thingToDo: TaskWithSubTasks) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(thingToDo.mainTask))
    }

    fun addThingToDo() = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToAddScreen)
    }

    // TODO: move in main activity
    fun onAddEditResult(result: Int, stringsAdded: Array<String>, stringsUpdated: Array<String>) {
        when (result) {
            ADD_TASK_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsAdded[0])
            ADD_EVENT_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsAdded[1])
            ADD_PROJECT_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsAdded[2])
            EDIT_TASK_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsUpdated[0])
            EDIT_PROJECT_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsUpdated[1])
            EDIT_EVENT_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsUpdated[2])
        }
    }

    fun onUndoDeleteClick(thingToDo: Ttd) = viewModelScope.launch {
        repository.insertTask(thingToDo.copy())
    }

    private fun showThingToDoSavedConfirmationMessage(text: String) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.ShowTaskSavedConfirmationMessage(text))
    }

    fun updateSubTask(subTask: Ttd) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(subTask))
    }

    fun deleteSubTask(subTask: Ttd) = viewModelScope.launch {
        repository.deleteTask(subTask)
        updateParentTask(subTask.parentTaskId!!)
        mainEventChannel.send(SharedEvent.ShowUndoDeleteTaskMessage(subTask))
    }

    fun navigateToTaskInfoScreen(thingToDo: Ttd) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToTaskViewPager(thingToDo))
    }

    fun navigateToTaskComposedInfoScreen(composedTask: TaskWithSubTasks) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToLocalProjectStatsScreen(composedTask))
    }

    fun navigateToTaskDetailsScreen(task: Ttd) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToTaskDetailsScreen(task))
    }

    fun lookForMissedRecurringTasks() = viewModelScope.launch {
        val todayDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
        val missedRecurringTasks: List<Ttd> = repository.getMissedRecurringTasks(todayDate)
        if (missedRecurringTasks.isNotEmpty()) {
            mainEventChannel.send(SharedEvent.ShowMissedRecurringTaskDialog(missedRecurringTasks))
        }
    }

    fun updateRecurringTasksMissed(missedTasks: List<Ttd>) = viewModelScope.launch {
        for (task in missedTasks) {
            val updatedTask = task.updateCheckedState(false)
            repository.updateTask(updatedTask)
        }
        showThingToDoSavedConfirmationMessage("Recurring tasks updated")
    }

    sealed class SharedEvent {
        data class NavigateToEditScreen(val thingToDo: Ttd) : SharedEvent()
        object NavigateToAddScreen : SharedEvent()

        /**
         * Event to navigate to view pager which display stats and time tracker for a specific task
         */
        data class NavigateToTaskViewPager(val task: Ttd) :
            SharedEvent()

        data class NavigateToTaskDetailsScreen(val task: Ttd) :
            SharedEvent()

        data class NavigateToLocalProjectStatsScreen(val composedTaskData: TaskWithSubTasks) :
            SharedEvent()

        data class ShowTaskSavedConfirmationMessage(val msg: String) : SharedEvent()
        data class ShowUndoDeleteTaskMessage(val thingToDo: Ttd) :
            SharedEvent()

        data class ShowMissedRecurringTaskDialog(val missedTasks: List<Ttd>) : SharedEvent()
    }
}
