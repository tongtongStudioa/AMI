package com.tongtongstudio.ami.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.LaterFilter
import com.tongtongstudio.ami.data.LayoutMode
import com.tongtongstudio.ami.data.PreferencesManager
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.receiver.ReminderBroadcastReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Calendar
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

    fun onHideLateClick(hideLate: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideLateTasks(hideLate)
    }

    fun onLaterFilterSelected(laterFilter: LaterFilter) = viewModelScope.launch {
        preferencesManager.updateLaterFilter(laterFilter)
    }

    fun onLayoutModeSelected(layoutMode: LayoutMode) = viewModelScope.launch {
        preferencesManager.updateLayoutMode(layoutMode)
    }

    fun onCheckBoxChanged(thingToDo: Task, checked: Boolean) = viewModelScope.launch {
        val updatedTask = thingToDo.updateCheckedState(checked)
        repository.updateTask(updatedTask)
        // update advancement project after adapt task
        if (updatedTask.parentTaskId != null) {
            updateParentTask(updatedTask.parentTaskId)
        }
    }

    fun updateParentTask(parentTaskId: Long?) = viewModelScope.launch {
        if (parentTaskId != null) {
            val taskWithSubTasks: ThingToDo = repository.getComposedTask(parentTaskId)
            val isCompleted =
                taskWithSubTasks.getNbSubTasks() == taskWithSubTasks.getNbSubTasksCompleted()
            val updatedParentTask = taskWithSubTasks.mainTask.updateCheckedState(isCompleted)
            repository.updateTask(updatedParentTask)
        }
    }

    fun deleteTask(thingToDo: ThingToDo, context: Context) = viewModelScope.launch {
        thingToDo.reminders.forEach { reminder ->
            cancelReminder(context, reminder.id)
        }
        repository.deleteTask(thingToDo.mainTask)
        if (thingToDo.mainTask.parentTaskId != null) {
            updateParentTask(thingToDo.mainTask.parentTaskId)
        }
        mainEventChannel.send(
            SharedEvent.ShowUndoDeleteTaskMessage(thingToDo.mainTask)
        )
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    fun updateTask(thingToDo: ThingToDo) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(thingToDo.mainTask))
    }

    fun addThingToDo() = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToAddScreen)
    }

    fun onUndoDeleteClick(thingToDo: Task) = viewModelScope.launch {
        repository.insertTask(thingToDo.copy())
    }

    fun showConfirmationMessage(result: Int) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.ShowConfirmationMessage(result))
    }

    fun updateSubTask(subTask: Task) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(subTask))
    }

    fun deleteSubTask(subTask: Task) = viewModelScope.launch {
        repository.deleteTask(subTask)
        updateParentTask(subTask.parentTaskId!!)
        mainEventChannel.send(SharedEvent.ShowUndoDeleteTaskMessage(subTask))
    }

    fun navigateToTaskInfoScreen(thingToDo: Task, sharedView: View) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToTaskViewPager(thingToDo, sharedView))
    }

    fun navigateToTaskComposedInfoScreen(composedTask: ThingToDo) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToLocalProjectStatsScreen(composedTask))
    }

    fun navigateToTaskDetailsScreen(task: Task, sharedView: View) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToTaskDetailsScreen(task, sharedView))
    }

    fun lookForMissedRecurringTasks() = viewModelScope.launch {
        val todayDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
        val missedRecurringTasks: List<ThingToDo> = repository.getMissedRecurringTasks(todayDate)
        if (missedRecurringTasks.isNotEmpty()) {
            mainEventChannel.send(SharedEvent.ShowMissedRecurringTaskDialog(missedRecurringTasks))
        }
    }

    fun updateRecurringTasksMissed(missedThingToDo: List<ThingToDo>) = viewModelScope.launch {
        for (thingToDo in missedThingToDo) {
            val updatedTask = thingToDo.mainTask.updateCheckedState(false)
            repository.updateTask(updatedTask)
        }
    }

    fun navigateToDraftTasksScreen() = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToDraftScreen)
    }

    fun updateTasksUrgency() = viewModelScope.launch {
        val todayDate = Calendar.getInstance().timeInMillis
        repository.updateTasksUrgency(todayDate)
    }

    sealed class SharedEvent {
        data class NavigateToEditScreen(val thingToDo: Task) : SharedEvent()
        data object NavigateToAddScreen : SharedEvent()

        /**
         * Event to navigate to view pager which display stats and time tracker for a specific task
         */
        data class NavigateToTaskViewPager(val task: Task, val sharedView: View) :
            SharedEvent()

        data class NavigateToTaskDetailsScreen(val task: Task, val sharedView: View) :
            SharedEvent()

        data class NavigateToLocalProjectStatsScreen(val composedTaskData: ThingToDo) :
            SharedEvent()

        data class ShowConfirmationMessage(val result: Int) : SharedEvent()
        data class ShowUndoDeleteTaskMessage(val thingToDo: Task) :
            SharedEvent()

        data class ShowMissedRecurringTaskDialog(val missedTasks: List<ThingToDo>) : SharedEvent()
        data object NavigateToDraftScreen : SharedEvent()
    }
}
