package com.tongtongstudio.ami.ui.edit.thingtodo

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.domain.usecase.ScheduleRemindersUseCase
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.EDIT_TASK_RESULT_OK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: Repository,
    private val scheduleReminders: ScheduleRemindersUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _addEditUiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _addEditUiState.asStateFlow()

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
            _addEditUiState.update { it.copy(isLoading = true) }
            try {
                _addEditUiState.update {
                    it.copy(categorySuggestions = categoriesSuggestions)
                }
            } catch (e: Exception) {
                _addEditUiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            _addEditUiState.update { it.copy(isLoading = true) }

            try {
                val thingToDo = savedStateHandle.get<ThingToDo>("thingToDo")
                val taskReminders: List<Reminder> = thingToDo?.taskRelations?.mainTask?.id?.let {
                    repository.getTaskReminders(it)?.first()
                } ?: emptyList()
                val category: Category? = thingToDo?.taskRelations?.category
                val blockingTask: Task? = thingToDo?.taskRelations?.taskDependency
                val parentProject: Task? = thingToDo?.taskRelations?.parentProject
                    ?: savedStateHandle.get<Task>("parent_task")
                val taskRecurrenceWithDays: TaskRecurrenceWithDays? =
                    thingToDo?.taskRelations?.mainTask?.recurrenceInfosId?.let {
                        repository.getTaskRecurrenceWithDays(it)
                    }
                _addEditUiState.update {
                    it.copy(
                        thingToDo = thingToDo,
                        title = thingToDo?.taskRelations?.mainTask?.title ?: "",
                        creationDateFormatted = thingToDo?.taskRelations?.mainTask?.getCreationDateFormatted(),
                        priority = thingToDo?.taskRelations?.mainTask?.priority,
                        importance = thingToDo?.taskRelations?.mainTask?.importance,
                        nature = thingToDo?.taskRelations?.mainTask?.nature, // getNature(it),
                        type = thingToDo?.getType(),
                        description = thingToDo?.taskRelations?.mainTask?.description ?: "",
                        startDate = thingToDo?.taskRelations?.mainTask?.startDate,
                        dueDate = thingToDo?.taskRelations?.mainTask?.dueDate,
                        deadline = thingToDo?.taskRelations?.mainTask?.deadline,
                        estimatedWorkTime = thingToDo?.taskRelations?.mainTask?.estimatedWorkingTime,
                        estimatedEmotions = thingToDo?.taskRelations?.mainTask?.estimatedEmotions
                            ?: 1,
                        skillLevel = thingToDo?.taskRelations?.mainTask?.skillLevel,
                        taskRecurrenceWithDays = taskRecurrenceWithDays,
                        reminders = taskReminders,
                        category = category,
                        blockingTask = blockingTask,
                        parentProject = parentProject,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("Error loading task", e.message.toString())
                _addEditUiState.update { it.copy(error = e.message) }
            }
            finally {
                _addEditUiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun getNature(uiState: AddEditUiState): String {
        val nature: Nature =
            if (uiState.parentProject != null && uiState.nature == Nature.PROJECT.name || uiState.nature == Nature.INTERMEDIATE_PROJECT.name)
                Nature.INTERMEDIATE_PROJECT
            else if (uiState.parentProject == null && uiState.nature == Nature.PROJECT.name)
                Nature.PROJECT
            else if (uiState.parentProject != null)
                Nature.SUB_TASK
            else Nature.TASK
        return nature.name
    }

    private fun restoreSavedState() {
        _addEditUiState.update { current ->
            current.copy(
                title = savedStateHandle.get<String>("thingToDoName") ?: current.title,
                priority = savedStateHandle.get<Int>("thingToDoPriority") ?: current.priority,
                creationDateFormatted = savedStateHandle.get<String>("creationDateFormatted")
                    ?: current.creationDateFormatted,
                nature = savedStateHandle.get<String>("thingToDoNature") ?: current.nature,
                description = savedStateHandle.get<String>("thingToDoDescription")
                    ?: current.description,
                importance = savedStateHandle.get<Int>("importance") ?: current.importance,
                urgency = savedStateHandle.get<Int>("urgency") ?: current.urgency,
                startDate = savedStateHandle.get<Long>("thingToDoStartDate") ?: current.startDate,
                dueDate = savedStateHandle.get<Long>("dueDate") ?: current.dueDate,
                deadline = savedStateHandle.get<Long>("thingToDoDeadline") ?: current.deadline,
                estimatedWorkTime = savedStateHandle.get<Long>("estimatedWorkingTime")
                    ?: current.estimatedWorkTime,
                parentProject = savedStateHandle.get<Task>("parentId") ?: current.parentProject,
                blockingTask = savedStateHandle.get<Task>("dependencyId") ?: current.blockingTask,
                category = savedStateHandle.get<Category>("thingToDoCategory") ?: current.category,
                skillLevel = savedStateHandle.get<Int>("level") ?: current.skillLevel,
                estimatedEmotions = savedStateHandle.get<Int>("estimatedEmotions")
                    ?: current.estimatedEmotions,
                taskRecurrenceWithDays = savedStateHandle.get<TaskRecurrenceWithDays>("taskRecurrenceWithDays")
                    ?: current.taskRecurrenceWithDays,
                reminders = savedStateHandle.get<List<Reminder>>("reminders") ?: current.reminders,
                isLoading = false // On restaure l'état donc le chargement est terminé
            )
        }
    }

    // Method to update fields
    fun updateTitle(title: String) {
        _addEditUiState.update { it.copy(title = title) }
        savedStateHandle["thingToDoName"] = title
    }

    fun updateNature(nature: String) {
        _addEditUiState.update { it.copy(nature = nature) }
        savedStateHandle["thingToDoNature"] = nature
        //Log.e("Task nature test", "Update task nature = $nature")
    }

    fun updateDescription(description: String?) {
        _addEditUiState.update { it.copy(description = description) }
        savedStateHandle["thingToDoDescription"] = description
    }

    // Méthodes pour les dates
    fun updateStartDate(date: Long?) {
        _addEditUiState.update { it.copy(startDate = date) }
        savedStateHandle["thingToDoStartDate"] = date
    }

    fun updateDueDate(date: Long?) {
        _addEditUiState.update { it.copy(dueDate = date) }
        savedStateHandle["dueDate"] = date
    }

    fun updateDeadline(date: Long?) {
        _addEditUiState.update { it.copy(deadline = date) }
        savedStateHandle["thingToDoDeadline"] = date
    }

    // Méthodes pour les relations
    fun updateParentProject(project: Task?) = viewModelScope.launch {
        _addEditUiState.update { it.copy(parentProject = project) }
        savedStateHandle["parentId"] = project
    }

    fun updateBlockingTask(blockingTask: Task?) {
        _addEditUiState.update { it.copy(blockingTask = blockingTask) }
        savedStateHandle["blockingTask"] = blockingTask
    }

    fun updateCategory(category: Category?) {
        _addEditUiState.update { it.copy(category = category) }
        savedStateHandle["thingToDoCategory"] = category
    }

    // Méthodes pour les métadonnées
    fun updateEstimatedWorkTime(time: Long?) {
        _addEditUiState.update { it.copy(estimatedWorkTime = time) }
        savedStateHandle["estimatedWorkingTime"] = time
    }

    fun updateEstimatedEmotions(emotions: Int) {
        _addEditUiState.update { it.copy(estimatedEmotions = emotions) }
        savedStateHandle["estimatedEmotions"] = emotions
    }

    fun updateSkillLevel(level: Int?) {
        _addEditUiState.update { it.copy(skillLevel = level) }
        savedStateHandle["level"] = level
    }

    fun updateImportance(importance: Int?) {
        _addEditUiState.update { it.copy(importance = importance) }
        savedStateHandle["importance"] = importance
    }

    fun updateRecurrenceInfos(
        taskRecurrenceWithDays: TaskRecurrenceWithDays?
    ) = viewModelScope.launch {
        _addEditUiState.update { state ->
            taskRecurrenceWithDays?.taskRecurrence?.copy(
                startDate = state.startDate,
                endDate = state.deadline,
            )
            state.copy(taskRecurrenceWithDays = taskRecurrenceWithDays)
        }
    }

    fun updateReminder(updatedReminder: Reminder) = viewModelScope.launch {
        _addEditUiState.update { state ->
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
        _addEditUiState.update { current ->
            current.copy(reminders = current.reminders + reminder)
        }
        savedStateHandle["reminders"] = _addEditUiState.value.reminders
    }

    fun removeReminder(reminder: Reminder) = viewModelScope.launch {
        _addEditUiState.update { current ->
            current.copy(reminders = current.reminders - reminder)
        }
        savedStateHandle["reminders"] = _addEditUiState.value.reminders
        if (reminder.parentId != null) repository.deleteReminder(reminder)
    }

    private suspend fun updateRemindersList(reminders: List<Reminder>, idTtd: Long) {
        for (reminder in reminders) {
            if (reminder.parentId == null || reminder.id == 0L) {
                Log.i("SAVE REMINDER", "Saving reminder")
                val newTaskReminder = reminder.copy(parentId = idTtd)
                repository.insertReminder(newTaskReminder)
            }
        }
    }

    fun setError(message: String?) {
        _addEditUiState.update { it.copy(error = message) }
    }

    fun saveTask(modeExtent: Boolean) = viewModelScope.launch {
        _addEditUiState.update { it.copy(isLoading = true) }
        try {
            withContext(Dispatchers.IO) {
                val taskRecurrenceWithDays = _addEditUiState.value.taskRecurrenceWithDays
                if (taskRecurrenceWithDays != null) {
                    val taskRecurrenceId =
                        repository.insertOrUpdateTaskRecurrenceWithDays(taskRecurrenceWithDays)
                    _addEditUiState.update {
                        it.copy(
                            taskRecurrenceWithDays = taskRecurrenceWithDays.copy(
                                taskRecurrenceWithDays.taskRecurrence.copy(
                                    recurrenceId = taskRecurrenceId
                                )
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _events.send(
                AddEditTaskEvent.ShowInvalidInputMessage(
                    e.message ?: "Error saving task recurrence infos"
                )
            )
        }
        val currentState = _addEditUiState.value
        val isUpdate = currentState.thingToDo != null
        try {
            withContext(Dispatchers.IO) {
                val task = buildTaskFromState(currentState, modeExtent)
                // get task id for saving reminders
                val taskId = if (isUpdate)
                    currentState.thingToDo.taskRelations.mainTask.id
                else repository.insertTask(task)

                // update if id is not null
                if (isUpdate)
                    repository.updateTask(task.copy(id = taskId))

                // update category of sub task
                repository.updateSubTasksCategory(
                    parentTaskId = taskId,
                    categoryId = currentState.category?.id
                )


                //Log.i("SAVE REMINDER", "Save reminder which hasn't yet saved !")
                // Save and schedule reminders
                updateRemindersList(currentState.reminders, taskId)
                //Log.i("SEND REMINDER NOTIF", "Schedule reminders use case!")
                scheduleReminders(currentState.reminders)
                //_events.send(AddEditTaskEvent.ScheduleReminders(currentState.reminders))
            }
        } catch (e: Exception) {
            setError(e.message)
            _events.send(
                AddEditTaskEvent.ShowInvalidInputMessage(
                    e.message ?: "Error saving task"
                )
            )
        } finally {
            if (_addEditUiState.value.error == null) {
                _addEditUiState.update { it.copy(isLoading = false) }
                val result = if (isUpdate) EDIT_TASK_RESULT_OK else ADD_TASK_RESULT_OK
                _events.send(AddEditTaskEvent.NavigateBackWithResult(result))
            }
        }
    }

    private fun buildTaskFromState(state: AddEditUiState, modeExtent: Boolean): Task {
        val isDraft = state.priority == null || state.dueDate == null
        val nature = getNature(state)
        return if (modeExtent) {
            val urgency = Task.calculusUrgency(
                Calendar.getInstance().timeInMillis,
                state.dueDate,
                state.deadline
            )
            Task(
                title = state.title,
                priority = Task.calculatingPriority(state.priority, state.importance, urgency),
                dueDate = state.dueDate,
                startDate = state.startDate,
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

    fun navigateToEditParentProject() = viewModelScope.launch(Dispatchers.IO) {
        _events.send(
            AddEditTaskEvent.NavigateToEditParentProjectDialog(
                _addEditUiState.value.thingToDo?.taskRelations?.mainTask?.id,
                uiState.value.parentProject
            )
        )
    }

    fun navigateToEditBlockingTaskDialog() = viewModelScope.launch {
        _events.send(
            AddEditTaskEvent.NavigateToEditBlockingTaskDialog(
                _addEditUiState.value.thingToDo?.taskRelations?.mainTask?.id,
                _addEditUiState.value.blockingTask
            )
        )
    }


    sealed class AddEditTaskEvent {
        data class ShowInvalidInputMessage(val msg: String) : AddEditTaskEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditTaskEvent()

        //object NavigatePickerDateScreen : AddEditTaskEvent()
        data class NavigateToEditParentProjectDialog(val taskId: Long?, val parentProject: Task?) :
            AddEditTaskEvent()

        data class NavigateToEditBlockingTaskDialog(val taskId: Long?, val blockingTask: Task?) :
            AddEditTaskEvent()
    }
}