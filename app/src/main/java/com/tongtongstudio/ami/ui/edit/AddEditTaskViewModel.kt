package com.tongtongstudio.ami.ui.edit

import androidx.lifecycle.*
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.*
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.EDIT_TASK_RESULT_OK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle
) : ViewModel() {


    private val addEditChannelEvent = Channel<AddEditTaskEvent>()
    val addEditTaskEvent = addEditChannelEvent.receiveAsFlow()

    // TODO: 21/06/2022 create a better variable for project id
    var projectId: Long? = null

    val thingToDo = state.get<Ttd>("thingToDo")

    private val _reminders = MutableLiveData<MutableList<Reminder>>()
    val reminders: LiveData<MutableList<Reminder>> get() = _reminders

    init {
        viewModelScope.launch {
            repository.getTaskReminders(thingToDo?.id)?.collect { reminders ->
                _reminders.value = reminders
            }
        }
    }

    private val _assessments = MutableLiveData<MutableList<Assessment>>()
    val assessments: LiveData<MutableList<Assessment>>
        get() = _assessments

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

    var category: Category? = getTaskCategory()

    var description: String? =
        state["ThingToDoDescription"] ?: thingToDo?.description
        set(value) {
            field = value
            state["ThingToDoDescription"] = value
        }

    var estimatedTime: Long? =
        state["estimatedTime"] ?: thingToDo?.estimatedTime
        set(value) {
            field = value
            state["estimatedTime"] = value
        }

    private var thingToDoIsSubTask =
        state.get<Boolean>("thingToDoIsSubTask") ?: (thingToDo?.parentTaskId != null)
        set(value) {
            field = value
            state["thingToDoIsSubTask"] = value
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
        state.get<String>("thingToDoNature") ?: thingToDo?.type
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

    fun onSaveClick() {
        thingToDo?.let {
            updateThingToDo(it)
            updateRemindersList(it.id)
            //updateAssessment(it.id)
        } ?: saveThingToDo()
    }

    private fun updateRemindersList(idTtd: Long) {
        if (reminders.value != null) {
            for (reminder in reminders.value!!) {
                if (reminder.parentId == null) {
                    val newTaskReminder = reminder.copy(parentId = idTtd)
                    insertReminder(newTaskReminder)
                }
            }
        }
    }

    fun updateReminder(oldReminder: Reminder, updatedReminder: Reminder) = viewModelScope.launch {
        if (oldReminder.parentId != null) {
            val currentReminders = _reminders.value ?: mutableListOf()
            val indexElement = currentReminders.indexOf(oldReminder)
            currentReminders.remove(oldReminder)
            currentReminders.add(indexElement, updatedReminder)
            _reminders.value = currentReminders
        } else
            repository.updateReminder(updatedReminder)
    }

    private fun insertReminder(reminder: Reminder) = viewModelScope.launch {
        repository.insertReminder(reminder)
    }

    fun updateCategoryId(title: String) {
        val updatedCategory = getTaskCategory(title)
        category = updatedCategory
        categoryId = updatedCategory?.id
    }

    private fun getTaskCategory(title: String? = null): Category? = runBlocking {
        return@runBlocking if (title != null)
            repository.getCategoryByTitle(title)
        else if (categoryId != null)
            repository.getCategoryById(categoryId!!)
        else null
    }

    private fun getCategories(): List<Category> = runBlocking {
        return@runBlocking repository.getCategories().first()
    }

    fun getCategoriesTitle(): List<String> {
        return getCategories().map { it.title }
    }

    fun getAssessments(): List<Assessment> = runBlocking {
        return@runBlocking if (thingToDo != null) {
            repository.getTasksAssessments(thingToDo.id).first()
        } else emptyList()
    }

    private fun insertNewAssessment(taskId: Long, newAssessment: Assessment) =
        viewModelScope.launch {
            repository.insertAssessment(newAssessment.copy(taskId = taskId))
        }

    private fun saveThingToDo() = viewModelScope.launch {
        val newThingToDo =
            Ttd(
                title,
                priority.toInt(),
                dueDate = dueDate!!,
                startDate = startDate,
                deadline = deadline,
                description = description,
                type = Nature.TASK.name,
                importance = importance,
                urgency = urgency,
                estimatedTime = estimatedTime,
                isRecurring = isRecurring,
                repetitionFrequency = recurringTaskInterval,
                dependency = dependency,
                skillLevel = skillLevel,
                parentTaskId = projectId,
                categoryId = categoryId
            )

        repository.insertTask(newThingToDo)

        // TODO: move out this method (to insure that update method assessment, categories and reminder is well saved

        addEditChannelEvent.send(
            AddEditTaskEvent.NavigateBackWithResult(
                ADD_TASK_RESULT_OK
            )
        )
    }

    private fun updateThingToDo(thingToDo: Ttd) =
        viewModelScope.launch {
            repository.updateTask(
                thingToDo.copy(
                    title,
                    priority.toInt(),
                    dueDate = dueDate!!,
                    startDate = startDate,
                    deadline = deadline,
                    description = description,
                    type = Nature.TASK.name,
                    importance = importance,
                    urgency = urgency,
                    estimatedTime = estimatedTime,
                    isRecurring = isRecurring,
                    repetitionFrequency = recurringTaskInterval,
                    dependency = dependency,
                    skillLevel = skillLevel,
                    parentTaskId = projectId,
                    categoryId = categoryId
                )
            )
            // TODO: move out this method (to insure that update method assessment, categories and reminder is well saved
            addEditChannelEvent.send(
                AddEditTaskEvent.NavigateBackWithResult(
                    EDIT_TASK_RESULT_OK
                )
            )
        }

    fun showInvalidInputMessage(invalidUserMsg: String) = viewModelScope.launch {
        addEditChannelEvent.send(AddEditTaskEvent.ShowInvalidInputMessage(invalidUserMsg))
    }

    fun addNewReminder(reminderTriggerTime: Long) {
        val newReminder = Reminder(
            dueDate = reminderTriggerTime,
            isRecurrent = false
        )
        val currentReminders = reminders.value ?: mutableListOf()
        currentReminders.add(newReminder)
        _reminders.value = currentReminders
    }

    fun addNewAssessment(result: Assessment) {
        _assessments.value?.add(result)
    }

    fun removeAssessment(assessment: Assessment) = viewModelScope.launch {
        _assessments.value?.remove(assessment)
        if (assessment.taskId == null)
            repository.deleteAssessment(assessment)
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