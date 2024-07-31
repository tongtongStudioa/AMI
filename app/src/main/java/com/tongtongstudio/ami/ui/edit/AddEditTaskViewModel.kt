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

    val thingToDo = state.get<Task>("thingToDo")
    private val _category = MutableLiveData<Category?>(null)
    val category: LiveData<Category?> // TODO: retrieve task's category with sage args
        get() = _category
    private val _reminders = MutableLiveData<MutableList<Reminder>>()
    val reminders: LiveData<MutableList<Reminder>>
        get() = _reminders


    init {
        viewModelScope.launch {
            repository.getTaskReminders(thingToDo?.id)?.collect { reminders ->
                _reminders.value = reminders
            }
            if (thingToDo?.categoryId != null)
                _category.value = repository.getCategoryById(thingToDo.categoryId)
        }
    }

    val createdDateFormatted = thingToDo?.getCreationDateFormatted()

    var title =
        state.get<String>("thingToDoName") ?: thingToDo?.title ?: ""
        set(value) {
            field = value
            state["thingToDoName"] = value
        }

    var priority =
        state.get<String>("ThingToDoPriority") ?: thingToDo?.priority.toString()
        set(value) {
            field = value
            state["thingToDoPriority"] = value
        }

    var categoryId: Long? =
        state["ThingToDoCategory"] ?: thingToDo?.categoryId
        set(value) {
            field = value
            state["ThingToDoCategory"] = value
        }

    var description: String? =
        state["ThingToDoDescription"] ?: thingToDo?.description
        set(value) {
            field = value
            state["ThingToDoDescription"] = value
        }

    var estimatedTime: Long? =
        state["estimatedWorkingTime"] ?: thingToDo?.estimatedWorkingTime
        set(value) {
            field = value
            state["estimatedWorkingTime"] = value
        }

    var startDate =
        state.get<Long>("thingToDoStartDate") ?: thingToDo?.startDate
        set(value) {
            field = value
            state["thingToDoStartDate"] = value
        }

    var dueDate =
        state.get<Long>("dueDate") ?: thingToDo?.dueDate
        set(value) {
            field = value
            state["dueDate"] = value
        }

    var deadline =
        state["thingToDoDeadline"] ?: thingToDo?.deadline
        set(value) {
            field = value
            state["thingToDoDeadline"] = value
        }

    var recurringTaskInterval =
        state["recurringTaskInterval"] ?: thingToDo?.repetitionFrequency
        set(value) {
            field = value
            state["recurringTaskInterval"] = value
        }

    var isRecurring = state.get<Boolean>("isRecurring") ?: thingToDo?.isRecurring ?: false
        set(value) {
            field = value
            state["isRecurring"] = value
        }

    var ttdNature =
        state.get<String>("thingToDoNature") ?: thingToDo?.type ?: Nature.TASK.name
        set(value) {
            field = value
            state["thingToDoNature"] = value
        }

    var dependency =
        state["dependency"] ?: thingToDo?.dependency
        set(value) {
            field = value
            state["dependency"] = value
        }

    var skillLevel =
        state["level"] ?: thingToDo?.skillLevel
        set(value) {
            field = value
            state["level"] = value
        }

    var importance =
        state["importance"] ?: thingToDo?.importance
        set(value) {
            field = value
            state["importance"] = value
        }

    var urgency =
        state["urgency"] ?: thingToDo?.urgency
        set(value) {
            field = value
            state["urgency"] = value
        }

    var projectId: Long? =
        state["parentId"] ?: thingToDo?.parentTaskId
        set(value) {
            field = value
            state["parentId"] = value
        }

    fun onSaveClick(modeExtent: Boolean) {
        thingToDo?.let {
            updateThingToDo(it, modeExtent)
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
        val newThingToDo =
            Task(
                title = title,
                priority = priority.toInt(),
                dueDate = dueDate!!,
                startDate = startDate,
                isRecurring = isRecurring,
                repetitionFrequency = recurringTaskInterval,
                parentTaskId = projectId,
                categoryId = categoryId,
                type = Nature.TASK.name
            )

        taskId = if (modeExtent) {
            urgency = Task.calculusUrgency(Calendar.getInstance().timeInMillis, dueDate!!, deadline)
            val newThingToDoExtent = newThingToDo.copy(
                priority = Task.calculatingPriority(priority.toInt(), importance, urgency),
                deadline = deadline,
                description = description,
                importance = importance,
                urgency = urgency,
                estimatedWorkingTime = estimatedTime,
                dependency = dependency,
                skillLevel = skillLevel,
                type = ttdNature
            )
            repository.insertTask(newThingToDoExtent)
        } else repository.insertTask(newThingToDo)

        updateRemindersList(taskId)

        // navigate back with result "OK"
        addEditChannelEvent.send(
            AddEditTaskEvent.NavigateBackWithResult(
                ADD_TASK_RESULT_OK
            )
        )
    }

    private fun updateThingToDo(thingToDo: Task, modeExtent: Boolean) =
        viewModelScope.launch {
            val updatedThingToDo =
                thingToDo.copy(
                    title = title,
                    priority = priority.toInt(),
                    dueDate = dueDate!!,
                    startDate = startDate,
                    isRecurring = isRecurring,
                    repetitionFrequency = recurringTaskInterval,
                    parentTaskId = projectId,
                    categoryId = categoryId,
                    type = Nature.TASK.name
                )
            if (modeExtent) {
                urgency =
                    Task.calculusUrgency(Calendar.getInstance().timeInMillis, dueDate!!, deadline)
                val updatedThingToDoExtent = updatedThingToDo.copy(
                    priority = Task.calculatingPriority(priority.toInt(), importance, urgency),
                    deadline = deadline,
                    description = description,
                    importance = importance,
                    urgency = urgency,
                    estimatedWorkingTime = estimatedTime,
                    dependency = dependency,
                    skillLevel = skillLevel,
                    type = ttdNature
                )
                repository.updateTask(updatedThingToDoExtent)
            } else repository.updateTask(updatedThingToDo)

            addEditChannelEvent.send(
                AddEditTaskEvent.NavigateBackWithResult(
                    EDIT_TASK_RESULT_OK
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
            isRecurrent = isRecurring,
            repetitionFrequency = recurringTaskInterval
        )
        if (thingToDo?.id == null) { // is a new task ?
            val currentReminders = reminders.value ?: mutableListOf()
            currentReminders.add(newReminder)
            _reminders.value = currentReminders
        } else insertNewReminder(newReminder.copy(parentId = thingToDo.id))
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