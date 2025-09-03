package com.tongtongstudio.ami.ui.edit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.DaysOfWeek
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.Type
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.EDIT_TASK_RESULT_OK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {


    data class UiState(
        // Données de base
        val thingToDo: ThingToDo? = null,
        val title: String = "",
        val description: String? = null,
        val creationDateFormatted: String? = null,
        val type: String? = Type.UNIQUE.name,

        // Dates
        val startDate: Long? = null,
        val dueDate: Long? = null,
        val deadline: Long? = null,

        // Priorité et importance
        val priority: Int? = null,
        val importance: Int? = null,
        val urgency: Int? = null,

        // Relations
        val parentProject: Task? = null,
        val blockingTask: Task? = null, //predecessorTask
        val category: Category? = null,

        // Métadonnées
        val nature: String? = Nature.TASK.name,
        val estimatedWorkTime: Long? = null,
        val estimatedEmotions: Int = 1,
        val skillLevel: Int? = null,
        val taskRecurrenceWithDays: TaskRecurrenceWithDays? = null,

        // Rappels
        val reminders: List<Reminder> = emptyList(),
        // Categories
        val categorySuggestions: List<Category> = emptyList(),

        // État UI
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    // Events
    private val _events = Channel<AddEditTaskEvent>()
    val addEditTaskEvent = _events.receiveAsFlow()

    init {
        observeCategories()
        loadInitialData()
        restoreSavedState()
    }

    private fun observeCategories() = viewModelScope.launch {
        repository.getCategories().collect { categoriesSuggestions ->
            _uiState.update { it.copy(isLoading = true) }
            try {
                _uiState.update {
                        it.copy(categorySuggestions = categoriesSuggestions)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val thingToDo = savedStateHandle.get<ThingToDo>("thingToDo")
                val taskReminders: List<Reminder> = thingToDo?.taskRelations?.mainTask?.id?.let { repository.getTaskReminders(it)?.first() } ?: emptyList()
                val category: Category? = thingToDo?.taskRelations?.category
                val blockingTask: Task? = thingToDo?.taskRelations?.taskDependency
                val parentProject: Task? = thingToDo?.taskRelations?.parentProject
                val recurringTaskInterval: TaskRecurrenceWithDays? = thingToDo?.taskRelations?.mainTask?.recurrenceInfosId?.let {repository.getTaskRecurrenceWithDays(it)}
                _uiState.update {
                    it.copy(
                        thingToDo = thingToDo,
                        title = thingToDo?.taskRelations?.mainTask?.title ?: "",
                        creationDateFormatted = thingToDo?.taskRelations?.mainTask?.getCreationDateFormatted(),
                        priority = thingToDo?.taskRelations?.mainTask?.priority,
                        nature = thingToDo?.getNature() ?: Nature.TASK.name,
                        type = thingToDo?.getType(),
                        description = thingToDo?.taskRelations?.mainTask?.description ?: "",
                        startDate = thingToDo?.taskRelations?.mainTask?.startDate,
                        dueDate = thingToDo?.taskRelations?.mainTask?.dueDate,
                        deadline = thingToDo?.taskRelations?.mainTask?.deadline,
                        estimatedWorkTime= thingToDo?.taskRelations?.mainTask?.estimatedWorkingTime,
                        estimatedEmotions = thingToDo?.taskRelations?.mainTask?.estimatedEmotions ?: 1,
                        skillLevel= thingToDo?.taskRelations?.mainTask?.skillLevel,
                        taskRecurrenceWithDays = recurringTaskInterval,
                        reminders = taskReminders,
                        category = category,
                        blockingTask = blockingTask,
                        parentProject = parentProject,
                        isLoading = false
                    )
                }
                _reminders.value = taskReminders
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun restoreSavedState() {
        // Restaurer chaque champ depuis SavedStateHandle
        _uiState.update { current ->
            current.copy(
                title = savedStateHandle.get<String>("thingToDoName") ?: current.title,
                priority = savedStateHandle.get<Int>("thingToDoPriority") ?: current.priority,
                creationDateFormatted = savedStateHandle.get<String>("creationDateFormatted") ?: current.creationDateFormatted,
                nature = savedStateHandle.get<String>("thingToDoNature") ?: current.nature,
                description = savedStateHandle.get<String>("thingToDoDescription") ?: current.description,
                importance = savedStateHandle.get<Int>("importance") ?: current.importance,
                urgency = savedStateHandle.get<Int>("urgency") ?: current.urgency,
                startDate = savedStateHandle.get<Long>("thingToDoStartDate") ?: current.startDate,
                dueDate = savedStateHandle.get<Long>("dueDate") ?: current.dueDate,
                deadline = savedStateHandle.get<Long>("thingToDoDeadline") ?: current.deadline,
                estimatedWorkTime = savedStateHandle.get<Long>("estimatedWorkingTime") ?: current.estimatedWorkTime,
                parentProject = savedStateHandle.get<Task>("parentId") ?: current.parentProject,
                blockingTask = savedStateHandle.get<Task>("dependencyId") ?: current.blockingTask,
                category = savedStateHandle.get<Category>("thingToDoCategory")?: current.category,
                skillLevel = savedStateHandle.get<Int>("level") ?: current.skillLevel,
                taskRecurrenceWithDays = savedStateHandle.get<TaskRecurrenceWithDays>("taskRecurrenceWithDays") ?: current.taskRecurrenceWithDays,
                reminders = savedStateHandle.get<List<Reminder>>("reminders") ?: current.reminders,
                isLoading = false // On restaure l'état donc le chargement est terminé
            )
        }
    }
    // Method to update fields
    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
        savedStateHandle["thingToDoName"] = title
    }

    fun updatePriority(priority: Int?) {
        _uiState.update { it.copy(priority = priority) }
        savedStateHandle["thingToDoPriority"] = priority
    }

    fun updateNature(nature: String) {
        _uiState.update { it.copy(nature = nature) }
        savedStateHandle["thingToDoNature"] = nature
        Log.e("Task nature test", " update task nature = $nature")
    }

    fun updateDescription(description: String?) {
        _uiState.update { it.copy(description = description) }
        savedStateHandle["thingToDoDescription"] = description
    }

    // Méthodes pour les dates
    fun updateStartDate(date: Long?) {
        _uiState.update { it.copy(startDate = date) }
        savedStateHandle["thingToDoStartDate"] = date
    }

    fun updateDueDate(date: Long?) {
        _uiState.update { it.copy(dueDate = date) }
        savedStateHandle["dueDate"] = date
    }

    fun updateDeadline(date: Long?) {
        _uiState.update { it.copy(deadline = date) }
        savedStateHandle["thingToDoDeadline"] = date
    }

    // Méthodes pour les relations
    fun updateParentProject(project: Task?) = viewModelScope.launch {
        _uiState.update { it.copy(parentProject = project) }
        savedStateHandle["parentId"] = project
    }

    fun updateDependencyTask(blockingTask: Task?) {
        _uiState.update { it.copy(blockingTask = blockingTask) }
        savedStateHandle["blockingTask"] = blockingTask
    }

    fun updateCategory(category: Category?) {
        _uiState.update { it.copy(category = category) }
        savedStateHandle["thingToDoCategory"] = category
    }

    // Méthodes pour les métadonnées
    fun updateEstimatedWorkTime(time: Long?) {
        _uiState.update { it.copy(estimatedWorkTime = time) }
        savedStateHandle["estimatedWorkingTime"] = time
    }

    fun updateSkillLevel(level: Int?) {
        _uiState.update { it.copy(skillLevel = level) }
        savedStateHandle["level"] = level
    }

    fun updateImportance(importance: Int?) {
        _uiState.update { it.copy(importance = importance) }
        savedStateHandle["importance"] = importance
    }

    fun updateUrgency(urgency: Int?) {
        _uiState.update { it.copy(urgency = urgency) }
        savedStateHandle["urgency"] = urgency
    }

    fun updateRecurrenceInfos(taskRecurrence: TaskRecurrence?, days: List<DaysOfWeek> = emptyList()) = viewModelScope.launch {
        var recurrenceInfos: TaskRecurrenceWithDays? = null
        if ((taskRecurrence == null))
            _uiState.update { it.copy( taskRecurrenceWithDays = null) }
        else {
            uiState.collect { state ->
                    val uTaskRecurrence = taskRecurrence.copy(
                    startDate = state.startDate,
                    endDate = state.deadline,
                )
                recurrenceInfos = TaskRecurrenceWithDays(uTaskRecurrence, days)
            }
        }
        _uiState.update { it.copy( taskRecurrenceWithDays = recurrenceInfos) }
        savedStateHandle["recurringTaskInterval"] = recurrenceInfos
    }

    fun updateReminder(updatedReminder: Reminder) = viewModelScope.launch {
        _uiState.update { state ->
            state.copy(
                reminders = state.reminders.map { oldReminder ->
                    if (oldReminder.id == updatedReminder.id) {
                        if (oldReminder.parentId == null) {
                            updatedReminder
                        } else {
                            // Update DB for pre register reminder modified
                            repository.updateReminder(updatedReminder.copy(parentId = oldReminder.parentId))
                            updatedReminder.copy(parentId = oldReminder.parentId)
                        }
                    } else oldReminder
                }
            )
        }
    }

    fun addReminder(reminder: Reminder) {
        _uiState.update { current ->
            current.copy(reminders = current.reminders + reminder)
        }
        savedStateHandle["reminders"] = _uiState.value.reminders
    }

    fun removeReminder(reminder: Reminder) = viewModelScope.launch {
        _uiState.update { current ->
            current.copy(reminders = current.reminders - reminder)
        }
        savedStateHandle["reminders"] = _uiState.value.reminders
        if (reminder.parentId != null) repository.deleteReminder(reminder)
    }

    fun updateCategorySearch(query: String) {
        _uiState.update { state ->
            state.copy(
                categorySuggestions = state.categorySuggestions
                    .filter { it.title.contains(query, ignoreCase = true) }
                    //.take(5) // Limite les suggestions
            )
        }
    }

    val isFormValid: StateFlow<Boolean> = _uiState.map { state ->
        state.title.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun saveTask(modeExtent: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val currentState = _uiState.value
                val task = buildTaskFromState(currentState, modeExtent)

                if (currentState.thingToDo == null) {
                    val taskId = repository.insertTask(task)
                    _events.send(AddEditTaskEvent.ScheduleReminders(_reminders.value))
                    _events.send(AddEditTaskEvent.NavigateBackWithResult(ADD_TASK_RESULT_OK))
                } else {
                    repository.updateTask(task.copy(id= currentState.thingToDo.taskRelations.mainTask.id))
                    _events.send(AddEditTaskEvent.ScheduleReminders(_reminders.value))
                    _events.send(AddEditTaskEvent.NavigateBackWithResult(EDIT_TASK_RESULT_OK))
                }
            } catch (e: Exception) {
                _events.send(AddEditTaskEvent.ShowInvalidInputMessage(e.message ?: "Error saving task"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun buildTaskFromState(state: UiState, modeExtent: Boolean): Task {
        val isDraft = state.priority == null || state.dueDate == null
        val nature = when {
            state.parentProject != null && state.nature == Nature.PROJECT.name -> Nature.INTERMEDIATE_PROJECT.name
            state.parentProject != null && state.nature == Nature.TASK.name -> Nature.SUB_TASK.name
            state.parentProject == null && state.nature == Nature.PROJECT.name -> Nature.PROJECT.name
            else -> Nature.TASK.name
        }
        return if (modeExtent) {
            val urgency = Task.calculusUrgency(Calendar.getInstance().timeInMillis, state.dueDate, state.deadline)
            Task(
                title = state.title,
                priority = Task.calculatingPriority(state.priority, state.importance, urgency),
                dueDate = state.dueDate,
                startDate =state.startDate,
                deadline = state.deadline,
                description = state.description,
                nature = nature,
                importance = state.importance,
                urgency = state.urgency,
                isDraft = isDraft,
                estimatedEmotions = state.estimatedEmotions,
                estimatedWorkingTime = state.estimatedWorkTime,
                skillLevel = state.skillLevel,
                dependencyId = state.blockingTask?.id,
                recurrenceInfosId = state.taskRecurrenceWithDays?.taskRecurrence?.recurrenceId,
                categoryId = state.category?.id,
                parentTaskId = state.parentProject?.id,
            )
        } else {
            Task(
                title = state.title,
                priority = state.priority,
                dueDate = state.dueDate,
                nature = nature,
                isDraft = isDraft,
                recurrenceInfosId = state.taskRecurrenceWithDays?.taskRecurrence?.recurrenceId,
            )
        }
    }

    sealed class AddEditTaskEvent {
        data class ShowInvalidInputMessage(val msg: String) : AddEditTaskEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditTaskEvent()
        //object NavigatePickerDateScreen : AddEditTaskEvent()
        data class ScheduleReminders(val reminders: List<Reminder>) : AddEditTaskEvent()
    }

    /*
    fun onSaveClick(modeExtent: Boolean) {
        thingToDo?.let { thingtoDo ->
            updateThingToDo(thingtoDo, modeExtent)
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
        val newThingToDo =
            Task(
                title = title,
                priority = priority,
                dueDate = dueDate,
                startDate = startDate,
                type = Nature.TASK.name,
                isDraft = isDraft,
                recurrenceInfosId = getRecurrenceId(),
                categoryId = categoryId,
                parentTaskId = projectId
            )

        taskId = if (modeExtent) {
            urgency = Task.calculusUrgency(Calendar.getInstance().timeInMillis, dueDate, deadline)
            val newThingToDoExtent = newThingToDo.copy(
                priority = Task.calculatingPriority(priority, importance, urgency),
                deadline = deadline,
                description = description,
                importance = importance,
                urgency = urgency,
                estimatedWorkingTime = estimatedTime,
                dependencyId = dependencyId,
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

    private fun updateThingToDo(thingToDo: ThingToDo, modeExtent: Boolean) =
        viewModelScope.launch {
            val updatedThingToDo =
                thingToDo.mainTask.copy(
                    title = title,
                    priority = priority,
                    dueDate = dueDate,
                    startDate = startDate,
                    recurrenceInfosId = recurringTaskInterval,
                    parentTaskId = projectId,
                    categoryId = categoryId,
                    isDraft = dueDate == null || priority == null,
                    type = Nature.TASK.name
                )
            if (modeExtent) {
                urgency =
                    Task.calculusUrgency(Calendar.getInstance().timeInMillis, dueDate, deadline)
                val updatedThingToDoExtent = updatedThingToDo.copy(
                    priority = Task.calculatingPriority(priority, importance, urgency),
                    deadline = deadline,
                    description = description,
                    importance = importance,
                    urgency = urgency,
                    estimatedWorkingTime = estimatedTime,
                    dependencyId = dependencyId,
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
        // TODO: find a new way for repeating reminder (update entity and migrate)
        val newReminder = Reminder(
            dueDate = reminderTriggerTime,
            isRecurrent = isRecurring,
            //repetitionFrequency = recurringTaskInterval
        )
        if (thingToDo?.mainTask?.id == null) { // is a new task ?
            val currentReminders = reminders.value ?: mutableListOf()
            currentReminders.add(newReminder)
            _reminders.value = currentReminders
        } else insertNewReminder(newReminder.copy(parentId = thingToDo.mainTask.id))
    }

    fun removeReminder(attribute: Reminder) = viewModelScope.launch {
        val updatedList: MutableList<Reminder> = _reminders.value ?: mutableListOf()
        val updatedList2 = updatedList - attribute
        _reminders.value = updatedList2
        if (attribute.parentId != null) repository.deleteReminder(attribute)
    }

   */
}