package com.tongtongstudio.ami.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.*
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.receiver.ReminderBroadcastReceiver
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

    // TODO: cancel  all reminders
    fun deleteTask(thingToDo: TaskWithSubTasks) = viewModelScope.launch {
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

    fun updateTask(thingToDo: TaskWithSubTasks) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(thingToDo.mainTask))
    }

    fun addThingToDo() = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToAddScreen)
    }

    fun onUndoDeleteClick(thingToDo: Ttd) = viewModelScope.launch {
        repository.insertTask(thingToDo.copy())
    }

    fun showConfirmationMessage(result: Int) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.ShowConfirmationMessage(result))
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

    fun showCompleteAssessmentDialog(assessment: Assessment) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.ShowCompleteAssessmentDialog(assessment))
    }

    fun updateRecurringTasksMissed(missedTasks: List<Ttd>) = viewModelScope.launch {
        for (task in missedTasks) {
            val updatedTask = task.updateCheckedState(false)
            repository.updateTask(updatedTask)
        }
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

        data class ShowConfirmationMessage(val result: Int) : SharedEvent()
        data class ShowUndoDeleteTaskMessage(val thingToDo: Ttd) :
            SharedEvent()

        data class ShowMissedRecurringTaskDialog(val missedTasks: List<Ttd>) : SharedEvent()

        data class ShowCompleteAssessmentDialog(val assessment: Assessment) : SharedEvent()
    }
}
