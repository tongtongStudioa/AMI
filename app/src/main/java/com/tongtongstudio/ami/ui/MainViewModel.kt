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
import com.tongtongstudio.ami.data.datatables.ProjectWithSubTasks
import com.tongtongstudio.ami.data.datatables.Task
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

    // sound Pool
    val soundPool: SoundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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

    fun onCheckBoxChanged(thingToDo: Ttd, checked: Boolean) = viewModelScope.launch {
        val updatedTask = updateTaskState(thingToDo, checked)
        repository.updateTask(updatedTask)
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
    private fun updateTaskState(task: Ttd, checked: Boolean): Ttd {
        val updatedTask = task.updateCheckedState(checked)
        // update advancement project after adapt task
        if (updatedTask.parentTaskId != null) {
            // TODO: update project state
        }
        return updatedTask
    }

    // TODO: 06/09/2022 change this method to show resource string
    fun onThingToDoRightSwiped(thingToDo: TaskWithSubTasks) = viewModelScope.launch {
        repository.deleteTask(thingToDo.mainTask)
        mainEventChannel.send(
            SharedEvent.ShowUndoDeleteTaskMessage(textShow = "Task deleted", thingToDo.mainTask)
        )
    }

    fun onThingToDoLeftSwiped(thingToDo: TaskWithSubTasks) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToEditScreen(thingToDo.mainTask))
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

    fun onSubTaskLeftSwiped(subTask: Ttd) = viewModelScope.launch {
        //mainEventChannel.send(SharedEvent.NavigateToEditScreen(subTask))
    }

    fun onSubTaskRightSwiped(subTask: Task) = viewModelScope.launch {
        //repository.deleteTask(subTask)
        /*if (subTask.projectId != null) {
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
        */
    }

    fun navigateToTaskInfoScreen(thingToDo: Ttd) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToTrackingScreen(thingToDo))
    }

    fun navigateToTaskComposedInfoScreen(composedTask: TaskWithSubTasks) = viewModelScope.launch {
        mainEventChannel.send(SharedEvent.NavigateToLocalProjectStatsScreen(composedTask))
    }

    fun lookForMissedRecurringTasks() = viewModelScope.launch {
        val todayDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            timeInMillis
        }
        val missedRecurringTasks: List<Ttd> = repository.getMissedRecurringTasks(todayDate)
        if (missedRecurringTasks.isNotEmpty()) {
            mainEventChannel.send(SharedEvent.ShowMissedRecurringTaskDialog(missedRecurringTasks))
        }
    }

    fun updateRecurringTasksMissed(missedTasks: List<Ttd>) = viewModelScope.launch {
        for (task in missedTasks) {
            updateTaskState(task, false)
        }
        showThingToDoSavedConfirmationMessage("Recurring tasks updated")
    }

    sealed class SharedEvent {
        data class NavigateToEditScreen(val thingToDo: Ttd) : SharedEvent()
        object NavigateToAddScreen : SharedEvent()
        data class NavigateToTrackingScreen(val task: Ttd) :
            SharedEvent()

        data class NavigateToLocalProjectStatsScreen(val composedTaskData: TaskWithSubTasks) :
            SharedEvent()

        data class ShowTaskSavedConfirmationMessage(val msg: String) : SharedEvent()
        data class ShowUndoDeleteTaskMessage(val textShow: String, val thingToDo: Ttd) :
            SharedEvent()

        data class ShowMissedRecurringTaskDialog(val missedTasks: List<Ttd>) : SharedEvent()
    }
}
