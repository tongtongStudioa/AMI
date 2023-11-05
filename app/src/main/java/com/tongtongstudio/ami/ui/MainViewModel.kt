package com.tongtongstudio.ami.ui

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.LaterFilter
import com.tongtongstudio.ami.data.PreferencesManager
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.Event
import com.tongtongstudio.ami.data.datatables.ProjectWithSubTasks
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.ui.dialog.Period
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

    // sound Pool
    val soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
    } else {
        SoundPool(6, AudioManager.STREAM_MUSIC, 0)
    }

    //val preferencesFlow = preferencesManager.preferencesFlow

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    fun onLaterFilterSelected(laterFilter: LaterFilter) = viewModelScope.launch {
        preferencesManager.updateLaterFilter(laterFilter)
    }

    fun onCheckBoxChanged(thingToDo: ThingToDo, checked: Boolean) = viewModelScope.launch {
        when (thingToDo) {
            is Task -> updateTask(thingToDo, checked)
        }
    }

    private fun updateEvent(isPassed: Boolean) {

    }

    private fun updateProjectAdvancement(projectData: ProjectWithSubTasks) = viewModelScope.launch {
        val projectLinked = projectData.project

        //adapt if the all project is completed and nb sub task completed
        var nbSubTasksCompleted = 0
        for (task in projectData.subTasks) {
            if (task.isTaskCompleted) {
                nbSubTasksCompleted++
            }
        }
        when {
            nbSubTasksCompleted == projectLinked.nb_sub_task -> {
                val completedDateInMillis = Calendar.getInstance().timeInMillis
                repository.updateProject(
                    projectLinked.copy(
                        isPjtCompleted = true,
                        nb_sub_tasks_completed = nbSubTasksCompleted,
                        pjtCompletedDate = completedDateInMillis
                    )
                )
            }
            // if nb sub tasks != nb sub task then pjt not completed
            projectLinked.isPjtCompleted -> {
                repository.updateProject(
                    projectLinked.copy(
                        isPjtCompleted = false,
                        nb_sub_tasks_completed = nbSubTasksCompleted,
                        pjtCompletedDate = null
                    )
                )
            }
            else -> repository.updateProject(projectLinked.copy(nb_sub_tasks_completed = nbSubTasksCompleted))
        }
    }

    // TODO: complete recurring task when start date > deadline date and don't update start date
    private suspend fun updateTask(task: Task, checked: Boolean) {
        val cloneTask: Task
        if (checked && task.isRecurring && task.taskStartDate != null && task.recurringTaskInterval != null) {
            val recurrenceDays =
                task.recurringTaskInterval.daysOfWeek // Custom recurrence days for this task
            var isTaskEnding = false
            val newStartDate = if (recurrenceDays != null) {
                // Set the new due date to the next occurrence of the task's due day
                Calendar.getInstance().run {
                    timeInMillis = task.taskStartDate
                    do {
                        add(Calendar.DAY_OF_WEEK, 1)
                        val nextDeadlineDay = get(Calendar.DAY_OF_WEEK)
                        if (get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) { // if we change of week so add intervalWeek if interval > 2 week
                            add(Calendar.DAY_OF_MONTH, (task.recurringTaskInterval.times - 1) * 7)
                        }
                    } while (!recurrenceDays.contains(nextDeadlineDay)) // if list of recurrence days contains next deadline's day so set this deadline
                    timeInMillis
                }
            } else {
                Calendar.getInstance().run {
                    timeInMillis = task.taskStartDate
                    when (task.recurringTaskInterval.period) {
                        Period.DAYS.name -> add(
                            Calendar.DAY_OF_MONTH,
                            task.recurringTaskInterval.times * 1
                        )
                        Period.WEEKS.name -> add(
                            Calendar.DAY_OF_MONTH,
                            task.recurringTaskInterval.times * 7
                        )
                        Period.MONTHS.name -> add(
                            Calendar.MONTH,
                            task.recurringTaskInterval.times * 1
                        )
                        Period.YEARS.name -> add(
                            Calendar.YEAR,
                            task.recurringTaskInterval.times * 1
                        )
                        else -> add(Calendar.DAY_OF_MONTH, 0)
                    }
                    timeInMillis
                }
            }
            // TODO: 25/10/2022 how dismiss a miss check ? how count when task were completed or not ?
            //val isCompleted = if (checked) 1 else 0
            val lastStartDate = Calendar.getInstance().run {
                timeInMillis = task.deadline!!
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                time
            }
            val completedDate = Calendar.getInstance().run {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                time
            }
            val currentStreak = if (lastStartDate >= completedDate) {
                task.streak + 1
            } else 0
            val newMaxStreak = if (currentStreak > task.maxStreak) currentStreak else task.maxStreak
            if (newStartDate > task.deadline!!) {
                isTaskEnding = true
            }
            cloneTask = task.copy(
                isTaskCompleted = isTaskEnding,
                taskStartDate = if (isTaskEnding) task.deadline else newStartDate,
                nbCompleted = task.nbCompleted + 1, // + isCompleted
                streak = currentStreak,
                maxStreak = newMaxStreak,
                nbRecurrence = task.nbRecurrence + 1
            )
            repository.updateTask(cloneTask)
        } else if (checked) {
            val completedDateInMillis = Calendar.getInstance().timeInMillis
            cloneTask = task.copy(
                isTaskCompleted = true,
                taskCompletedDate = completedDateInMillis
            )
        } else {
            cloneTask = task.copy(
                isTaskCompleted = false,
                taskCompletedDate = null
            )
        }
        repository.updateTask(cloneTask)
        // adapt advancement project after adapt task
        if (cloneTask.projectId != null) {
            val projectLinked = repository.getProjectData(cloneTask.projectId)
            updateProjectAdvancement(projectLinked)
        }
    }

    // TODO: 06/09/2022 change this method to show resource string
    fun onThingToDoRightSwiped(thingToDo: ThingToDo) = viewModelScope.launch {
        var textShow = ""
        when (thingToDo) {
            is Task -> {
                repository.deleteTask(thingToDo)
                if (thingToDo.projectId != null) {
                    val projectData = repository.getProjectData(thingToDo.projectId)
                    val projectLinked = projectData.project
                    val isSubTaskCompleted = if (thingToDo.isTaskCompleted) 1 else 0
                    repository.updateProject(
                        projectLinked.copy(
                            nb_sub_tasks_completed = projectLinked.nb_sub_tasks_completed - isSubTaskCompleted,
                            nb_sub_task = projectLinked.nb_sub_task - 1
                        )
                    )
                }
                textShow = "Task deleted"
            }
            is ProjectWithSubTasks -> {
                repository.deleteProject(thingToDo.project)
                repository.deleteSubTasks(thingToDo.subTasks)
                textShow = "Project deleted"
            }
            is Event -> {
                repository.deleteEvent(thingToDo)
                textShow = "Event deleted"
            }
        }
        mainEventChannel.send(
            SharedEvent.ShowUndoDeleteTaskMessage(
                textShow,
                thingToDo
            )
        )
    }

    fun onThingToDoLeftSwiped(thingToDo: ThingToDo) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(thingToDo))
    }

    fun onAddThingToDoDemand() = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToAddScreen)
    }

    fun onAddEditResult(result: Int, stringsAdded: Array<String>, stringsUpdated: Array<String>) {
        when (result) {
            ADD_TASK_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsAdded[0])
            ADD_EVENT_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsAdded[1])
            ADD_PROJECT_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsAdded[2])
            EDIT_TASK_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsUpdated[0])
            EDIT_PROJECT_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsUpdated[0])
            EDIT_EVENT_RESULT_OK -> showThingToDoSavedConfirmationMessage(stringsUpdated[0])
        }
    }

    fun onUndoDeleteClick(thingToDo: ThingToDo) = viewModelScope.launch {
        when (thingToDo) {
            is ProjectWithSubTasks -> {
                repository.insertProject(thingToDo.project.copy())
                thingToDo.subTasks.forEach {
                    repository.insertTask(it.copy())
                }
            }
            is Task -> {
                repository.insertTask(thingToDo.copy())
            }
            is Event -> {
                repository.insertEvent(thingToDo.copy())
            }
        }
    }

    private fun showThingToDoSavedConfirmationMessage(text: String) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.ShowTaskSavedConfirmationMessage(text))
    }

    fun onSubTaskLeftSwiped(subTask: Task) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(subTask))
    }

    fun onSubTaskRightSwiped(subTask: Task) = viewModelScope.launch {
        repository.deleteTask(subTask)
        if (subTask.projectId != null) {
            val projectData = repository.getProjectData(subTask.projectId)
            val projectLinked = projectData.project
            val isSubTaskCompleted = if (subTask.isTaskCompleted) 1 else 0
            repository.updateProject(
                projectLinked.copy(
                    nb_sub_tasks_completed = projectLinked.nb_sub_tasks_completed - isSubTaskCompleted,
                    nb_sub_task = projectLinked.nb_sub_task - 1
                )
            )
        }
        mainEventChannel.send(SharedEvent.ShowUndoDeleteTaskMessage("Sub task deleted", subTask))
    }

    fun onThingToDoClicked(thingToDo: ThingToDo) = viewModelScope.launch {
        when (thingToDo) {
            is Task -> mainEventChannel.send(SharedEvent.NavigateToTrackingScreen(thingToDo))
            is ProjectWithSubTasks -> mainEventChannel.send(
                SharedEvent.NavigateToLocalProjectStatsScreen(
                    thingToDo
                )
            )
        }
    }

    fun lookForMissedRecurringTasks() = viewModelScope.launch {
        val todayDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            timeInMillis
        }
        val missedRecurringTasks: List<Task> = repository.getMissedRecurringTasks(todayDate)
        if (missedRecurringTasks.isNotEmpty()) {
            mainEventChannel.send(SharedEvent.ShowMissedRecurringTaskDialog(missedRecurringTasks))
        }
    }

    fun updateRecurringTasksMissed(missedTasks: List<Task>) = viewModelScope.launch {
        val todayTimeInMillis = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            timeInMillis
        }
        for (task in missedTasks) {
            val recurrenceDays =
                task.recurringTaskInterval!!.daysOfWeek // Custom recurrence days for this task
            var timesSkipped = 0 // to analyse recurrent task and capacity to stick to the routine
            var isCompleted = false
            val newStartDate = if (recurrenceDays != null) {
                // Set the new start date to the next occurrence after today
                Calendar.getInstance().run {
                    // TODO:  also update startDate
                    timeInMillis = task.taskStartDate!!
                    do {
                        if (recurrenceDays.contains(get(Calendar.DAY_OF_WEEK)))
                            timesSkipped++
                        add(Calendar.DAY_OF_WEEK, 1)
                        val nextStartDateDay = get(Calendar.DAY_OF_WEEK)
                        if (get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) { // if we change of week so add intervalWeek if interval > 2 week
                            add(Calendar.DAY_OF_MONTH, (task.recurringTaskInterval.times - 1) * 7)
                        }
                    } while (!recurrenceDays.contains(nextStartDateDay) || timeInMillis < todayTimeInMillis)
                    // if list of recurrence days contains next startDate's day and it's not before today so set new start date
                    timeInMillis
                }
            } else {
                Calendar.getInstance().run {
                    timeInMillis = task.taskStartDate!!
                    do {
                        when (task.recurringTaskInterval.period) {
                            Period.DAYS.name -> add(
                                Calendar.DAY_OF_MONTH,
                                task.recurringTaskInterval.times * 1
                            )
                            Period.WEEKS.name -> add(
                                Calendar.DAY_OF_MONTH,
                                task.recurringTaskInterval.times * 7
                            )
                            Period.MONTHS.name -> add(
                                Calendar.MONTH,
                                task.recurringTaskInterval.times * 1
                            )
                            Period.YEARS.name -> add(
                                Calendar.YEAR,
                                task.recurringTaskInterval.times * 1
                            )
                            else -> add(Calendar.DAY_OF_MONTH, 0)
                        }
                        timesSkipped++
                    } while (timeInMillis < todayTimeInMillis)
                    timeInMillis
                }
            }
            if (newStartDate > (task.deadline ?: todayTimeInMillis))
                isCompleted = true
            val cloneTask = task.copy(
                isTaskCompleted = isCompleted,
                taskStartDate = if (isCompleted) task.deadline else newStartDate,
                streak = 0,
                nbRecurrence = task.nbRecurrence + timesSkipped,
            )
            repository.updateTask(cloneTask)
        }
        showThingToDoSavedConfirmationMessage("Recurring tasks updated")
    }

    sealed class SharedEvent {
        data class NavigateToEditScreen(val thingToDo: ThingToDo) : SharedEvent()
        object NavigateToAddScreen : SharedEvent()
        data class NavigateToTrackingScreen(val task: Task) :
            SharedEvent()

        data class NavigateToLocalProjectStatsScreen(val projectData: ProjectWithSubTasks) :
            SharedEvent()

        data class ShowTaskSavedConfirmationMessage(val msg: String) : SharedEvent()
        data class ShowUndoDeleteTaskMessage(val textShow: String, val thingToDo: ThingToDo) :
            SharedEvent()

        data class ShowMissedRecurringTaskDialog(val missedTasks: List<Task>) : SharedEvent()
    }
}
