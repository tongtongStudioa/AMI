package com.tongtongstudio.ami.ui.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.ui.ADD_DRAFT_TASK_OK
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.EDIT_TASK_RESULT_OK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle
) : ViewModel() {

    private val addEditChannelEvent = Channel<AddEditTaskEvent>()
    val addEditTaskEvent = addEditChannelEvent.receiveAsFlow()

    val thingToDo = state.get<ThingToDo>("thingToDo")
    private val _category = MutableLiveData<Category?>(thingToDo?.category)
    val category: LiveData<Category?>
        get() = _category
    private val _reminders = MutableLiveData<MutableList<Reminder>>((thingToDo?.reminders)?.toMutableList())
    val reminders: LiveData<MutableList<Reminder>>
        get() = _reminders


    val createdDateFormatted = thingToDo?.mainTask?.getCreationDateFormatted()

    var title =
        state.get<String>("thingToDoName") ?: thingToDo?.mainTask?.title ?: ""
        set(value) {
            field = value
            state["thingToDoName"] = value
        }

    var priority: Int? =
        state.get<Int>("thingToDoPriority") ?: thingToDo?.mainTask?.priority
        set(value) {
            field = value
            state["thingToDoPriority"] = value
        }

    var categoryId: Long? =
        state["ThingToDoCategory"] ?: thingToDo?.mainTask?.categoryId
        set(value) {
            field = value
            state["ThingToDoCategory"] = value
        }

    var description: String? =
        state["thingToDoDescription"] ?: thingToDo?.mainTask?.description
        set(value) {
            field = value
            state["thingToDoDescription"] = value
        }

    var estimatedTime: Long? =
        state["estimatedWorkingTime"] ?: thingToDo?.mainTask?.estimatedWorkingTime
        set(value) {
            field = value
            state["estimatedWorkingTime"] = value
        }

    var startDate =
        state.get<Long>("thingToDoStartDate") ?: thingToDo?.mainTask?.startDate
        set(value) {
            field = value
            state["thingToDoStartDate"] = value
        }

    var dueDate =
        state.get<Long>("dueDate") ?: thingToDo?.mainTask?.dueDate
        set(value) {
            field = value
            state["dueDate"] = value
        }

    var deadline =
        state["thingToDoDeadline"] ?: thingToDo?.mainTask?.deadline
        set(value) {
            field = value
            state["thingToDoDeadline"] = value
        }

    var taskRecurrenceWithDays: TaskRecurrenceWithDays? =
        state["taskRecurrenceWithDays"] ?: thingToDo?.recurrence
        set(value) {
            field = value
            state["taskRecurrenceWithDays"] = value
        }

    var taskRecurrence: TaskRecurrence? =
        state["taskRecurrence"] ?: thingToDo?.recurrence?.taskRecurrence
        set(value) {
            field = value
            state["taskRecurrence"] = value
        }

    var ttdNature =
        state.get<String>("thingToDoNature") ?: thingToDo?.mainTask?.type ?: Nature.TASK.name
        set(value) {
            field = value
            state["thingToDoNature"] = value
        }

    var taskDependency =
        state["dependencies"] ?: thingToDo?.taskDependency
        set(value) {
            field = value
            state["dependencies"] = value
        }

    var skillLevel =
        state["level"] ?: thingToDo?.mainTask?.skillLevel
        set(value) {
            field = value
            state["level"] = value
        }

    var importance =
        state["importance"] ?: thingToDo?.mainTask?.importance
        set(value) {
            field = value
            state["importance"] = value
        }

    var urgency =
        state["urgency"] ?: thingToDo?.mainTask?.urgency
        set(value) {
            field = value
            state["urgency"] = value
        }

    var projectId: Long? =
        state["parentId"] ?: thingToDo?.mainTask?.parentTaskId
        set(value) {
            field = value
            state["parentId"] = value
        }

    fun onSaveClick(modeExtent: Boolean) {
        thingToDo?.let {
            updateThingToDo(it.mainTask, modeExtent)
        } ?: saveThingToDo(modeExtent)
    }

    private fun updateRemindersList(idTtd: Long) {
        if (reminders.value != null) {
            for (reminder in reminders.value!!) {
                if (reminder.parentId == null) {
                    val newTaskReminder = reminder.copy(parentId = idTtd)
                    insertNewReminder(newTaskReminder)
                }
            }
        }
    }

    fun updateReminder(oldReminder: Reminder, updatedReminder: Reminder) = viewModelScope.launch {
        if (oldReminder.parentId == null) {
            val currentReminders = _reminders.value ?: mutableListOf()
            val indexElement = currentReminders.indexOf(oldReminder)
            currentReminders.remove(oldReminder)
            currentReminders.add(indexElement, updatedReminder)
            _reminders.value = currentReminders
        } else
            repository.updateReminder(updatedReminder)
    }

    private fun insertNewReminder(reminder: Reminder) = viewModelScope.launch {
        repository.insertReminder(reminder)
    }

    fun updateCategory(updatedCategory: Category?) {
        _category.value = updatedCategory
        categoryId = updatedCategory?.id
    }

    fun getCategories() = repository.getCategories().asLiveData()

    private fun saveThingToDo(modeExtent: Boolean) = viewModelScope.launch {
        val taskId: Long
        val isDraft = dueDate == null || priority == null
        // TODO: add new fields : recurrenceId and emotions and dependencies !
        val newThingToDo =
            Task(
                title = title,
                priority = priority,
                dueDate = dueDate,
                startDate = startDate,
                type = Nature.TASK.name,
                isDraft = isDraft,
                categoryId = categoryId,
                parentTaskId = projectId
            )

        taskId = if (modeExtent) {
            urgency = if (isDraft) 1 else Task.calculusUrgency(Calendar.getInstance().timeInMillis, dueDate!!, deadline)
            val newThingToDoExtent = newThingToDo.copy(
                priority = Task.calculatingPriority(priority, importance, urgency),
                deadline = deadline,
                description = description,
                importance = importance,
                urgency = urgency,
                estimatedWorkingTime = estimatedTime,
                dependencyId = taskDependency?.mainTask?.id,
                skillLevel = skillLevel,
                type = ttdNature
            )
            repository.insertTask(newThingToDoExtent)
        } else repository.insertTask(newThingToDo)

        updateRemindersList(taskId)

        // navigate back with result "OK"
        addEditChannelEvent.send(
            AddEditTaskEvent.NavigateBackWithResult(
                if (isDraft) ADD_DRAFT_TASK_OK else ADD_TASK_RESULT_OK
            )
        )
    }

    private fun updateThingToDo(thingToDo: Task, modeExtent: Boolean) =
        viewModelScope.launch {
            val isDraft = dueDate == null || priority == null
            // TODO: add new fields : recurrenceId and emotions and dependencies !
            val updatedThingToDo =
                thingToDo.copy(
                    title = title,
                    priority = priority,
                    dueDate = dueDate,
                    startDate = startDate,
                    parentTaskId = projectId,
                    categoryId = categoryId,
                    isDraft = dueDate == null || priority == null,
                    type = Nature.TASK.name
                )
            if (modeExtent) {
                urgency =
                    if (isDraft) 1 else Task.calculusUrgency(Calendar.getInstance().timeInMillis, dueDate!!, deadline)
                val updatedThingToDoExtent = updatedThingToDo.copy(
                    priority = Task.calculatingPriority(priority, importance, urgency),
                    deadline = deadline,
                    description = description,
                    importance = importance,
                    urgency = urgency,
                    estimatedWorkingTime = estimatedTime,
                    dependencyId = taskDependency?.mainTask?.id,
                    skillLevel = skillLevel,
                    type = ttdNature
                )
                repository.updateTask(updatedThingToDoExtent)
            } else repository.updateTask(updatedThingToDo)

            addEditChannelEvent.send(
                AddEditTaskEvent.NavigateBackWithResult(
                    // TODO: change this result code for draft thingToDo
                    if (isDraft) EDIT_TASK_RESULT_OK else EDIT_TASK_RESULT_OK
                )
            )
        }

    fun showInvalidInputMessage(invalidUserMsg: String) = viewModelScope.launch {
        addEditChannelEvent.send(AddEditTaskEvent.ShowInvalidInputMessage(invalidUserMsg))
    }

    fun getMainTask(): String = runBlocking {
        return@runBlocking if (projectId != null) {
            repository.getTask(projectId!!).title
        } else "null"
    }

    fun addNewReminder(reminderTriggerTime: Long) {
        val newReminder = Reminder(
            dueDate = reminderTriggerTime,
            // TODO: change this with another migration
            isRecurrent = false
            //repetitionFrequency = taskRecurrenceWithDays.taskRecurrence.recurrenceId
        )
        if (thingToDo?.mainTask?.id == null) { // is a new thingToDo ?
            val currentReminders = reminders.value ?: mutableListOf()
            currentReminders.add(newReminder)
            _reminders.value = currentReminders
        } else insertNewReminder(newReminder.copy(parentId = thingToDo.mainTask.id))
    }

    fun removeReminder(attribute: Reminder) = viewModelScope.launch {
        val updatedList: MutableList<Reminder> = _reminders.value ?: mutableListOf()
        updatedList.remove(attribute)
        _reminders.value = updatedList
        if (attribute.parentId != null) repository.deleteReminder(attribute)
    }

    sealed class AddEditTaskEvent {
        data class ShowInvalidInputMessage(val msg: String) : AddEditTaskEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditTaskEvent()
        //object NavigatePickerDateScreen : AddEditTaskEvent()
    }
}