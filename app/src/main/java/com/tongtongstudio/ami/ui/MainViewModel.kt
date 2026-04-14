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
import kotlinx.coroutines.Dispatchers
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

    fun onCheckBoxChanged(thingToDo: ThingToDo, isChecked: Boolean) = viewModelScope.launch(
        Dispatchers.IO) {
        val lastCompletion: Boolean = thingToDo.lastCompletionStatus ?: false
        //Log.e("OnCheckBoxChanged", "Inside on check box changed !")
        repository.toggleTaskCompletion(thingToDo.getType(), thingToDo.taskRelations.mainTask, isChecked, lastCompletion)
        //Log.e("OnCheckBoxChanged", "toggle task completion finish !")
    }

    fun deleteTask(thingToDo: ThingToDo, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        /*val reminders = repository.getTaskReminders(thingToDo.taskRelations.mainTask.id)?.collect() { reminders ->
            reminders.forEach {
                cancelReminder(context, it.id)
            }
        }*/
        repository.deleteTask(thingToDo.taskRelations.mainTask)
        mainEventChannel.send(
            SharedEvent.ShowUndoDeleteTaskMessage(thingToDo.taskRelations.mainTask)
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

    fun updateTask(thingToDo: ThingToDo) = viewModelScope.launch(Dispatchers.IO) {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(thingToDo))
    }

    fun addThingToDo() = viewModelScope.launch(Dispatchers.IO) {
        mainEventChannel.send(SharedEvent.NavigateToAddScreen)
    }

    fun addSubThingTodo(parentTask: Task) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToAddScreenWithParentTask(parentTask))
    }

    fun onUndoDeleteClick(thingToDo: Task) = viewModelScope.launch {
        repository.insertTask(thingToDo.copy())
    }

    fun showConfirmationMessage(result: Int) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.ShowConfirmationMessage(result))
    }

    fun updateSubTask(subTask: ThingToDo) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(subTask))
    }

    fun deleteSubTask(subTask: Task) = viewModelScope.launch {
        repository.deleteTask(subTask)
        mainEventChannel.send(SharedEvent.ShowUndoDeleteTaskMessage(subTask))
    }

    fun navigateToTaskDetailsAndTrackScreen(thingToDo: Task, sharedView: View, position: Int = -1) = viewModelScope.launch(
        Dispatchers.IO) {
        mainEventChannel.send(SharedEvent.NavigateToTaskViewPager(thingToDo, sharedView, position))
    }

    fun navigateToProjectDetailsScreen(project: ThingToDo, sharedView: View, position: Int = -1) = viewModelScope.launch(
        Dispatchers.IO) {
        mainEventChannel.send(SharedEvent.NavigateToProjectDetailsScreen(project,sharedView, position))
    }

    fun navigateToTaskDetailsScreen(task: Task, sharedView: View) = viewModelScope.launch(
        Dispatchers.IO) {
        mainEventChannel.send(SharedEvent.NavigateToTaskDetailsScreen(task, sharedView))
    }

    fun lookForMissedRecurringTasks() = viewModelScope.launch(Dispatchers.IO) {
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
            //Log.e("Update recurring task", thingToDo.taskRelations.mainTask.title + "on checked call")
            onCheckBoxChanged(thingToDo, false)
        }
    }

    fun updateTasksUrgency() = viewModelScope.launch {
        val todayDate = Calendar.getInstance().timeInMillis
        repository.updateTasksUrgency(todayDate)
    }

    sealed class SharedEvent {
        data class NavigateToEditScreen(val thingToDo: ThingToDo) : SharedEvent()
        data object NavigateToAddScreen : SharedEvent()

        /**
         * Event to navigate to view pager which display stats and time tracker for a specific task
         */
        data class NavigateToTaskViewPager(val task: Task, val sharedView: View, val position: Int = -1) :
            SharedEvent()

        data class NavigateToTaskDetailsScreen(val task: Task, val sharedView: View) :
            SharedEvent()

        data class NavigateToProjectDetailsScreen(val project: ThingToDo, val sharedView: View, val position: Int = -1) :
            SharedEvent()

        data class ShowConfirmationMessage(val result: Int) : SharedEvent()
        data class ShowUndoDeleteTaskMessage(val thingToDo: Task) :
            SharedEvent()

        data class ShowMissedRecurringTaskDialog(val missedTasks: List<ThingToDo>) : SharedEvent()
        class NavigateToAddScreenWithParentTask(val parentTask: Task) : SharedEvent()

        data object NavigateToDraftScreen : SharedEvent()
    }
}
