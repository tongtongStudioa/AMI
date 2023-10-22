package com.tongtongstudio.ami.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.RecurringTaskInterval
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.*
import com.tongtongstudio.ami.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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

    val thingToDo = state.get<ThingToDo>("thingToDo")

    val createdDateFormatted = thingToDo?.getCreatedDateFormatted()

    var name =
        state.get<String>("thingToDoName") ?: thingToDo?.name ?: ""
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

    var category: String? =
        state["ThingToDoCategory"] ?: thingToDo?.getCategory()
        set(value) {
            field = value
            state["ThingToDoCategory"] = value
        }

    var description: String? =
        state["ThingToDoDescription"] ?: thingToDo?.getDescription()
        set(value) {
            field = value
            state["ThingToDoDescription"] = value
        }

    var estimatedTime: Long? =
        state["estimatedTime"] ?: thingToDo?.getEstimatedTime()
        set(value) {
            field = value
            state["estimatedTime"] = value
        }

    private var thingToDoIsSubTask =
        state.get<Boolean>("thingToDoIsSubTask") ?: when (thingToDo) {
            is Task -> thingToDo.projectId != null
            else -> false
        }
        set(value) {
            field = value
            state["thingToDoIsSubTask"] = value
        }

    var reminder: Long? =
        state.get<Long>("reminder") ?: thingToDo?.getReminderDate()
        set(value) {
            field = value
            state["reminder"] = value
        }

    var startDate =
        state.get<Long>("thingToDoStartDate") ?: thingToDo?.getStartDate()
        set(value) {
            field = value
            state["thingToDoStartDate"] = value
        }

    var deadline =
        state["thingToDoDeadline"] ?: thingToDo?.deadline
        set(value) {
            field = value
            state["thingToDoDeadline"] = value
        }

    // TODO: 31/10/2022 make all type thing to do repeatable
    var recurringTaskInterval =
        state.get<RecurringTaskInterval>("recurringTaskInterval") ?: when (thingToDo) {
            is Task -> thingToDo.recurringTaskInterval
            else -> null
        }
        set(value) {
            field = value
            state["recurringTaskInterval"] = value
        }

    var isRecurring = state.get<Boolean>("isRecurring") ?: when (thingToDo) {
        is Task -> thingToDo.isRecurring
        else -> false
    }
        set(value) {
            field = value
            state["isRecurring"] = value
        }

    var eventIsSpread =
        state.get<Boolean>("eventIsSpread") ?: when (thingToDo) {
            is Event -> thingToDo.isSpread
            else -> false
        }
        set(value) {
            field = value
            state["eventIsSpread"] = value
        }

    var evaluationTaskDescription: String? =
        state["evaluationDescription"] ?: if (thingToDo is Task)
            thingToDo.taskEvaluationDescription
        else null

    var evaluationTaskGoal: Double? =
        state["evaluationRating"] ?: if (thingToDo is Task)
            thingToDo.taskEvaluationGoal
        else null

    var evaluationTaskUnit: String? =
        state["evaluationUnit"] ?: if (thingToDo is Task)
            thingToDo.taskEvaluationUnit
        else null

    var evaluationTaskDate: Long? =
        state["evaluationDate"] ?: if (thingToDo is Task)
            thingToDo.taskEvaluationDate
        else null

    var ttdNature =
        state.get<Nature>("thingToDoNature") ?: when (thingToDo) {
            is Task -> Nature.TASK
            is ProjectWithSubTasks -> Nature.PROJECT
            is Event -> Nature.EVENT
            else -> Nature.TASK
        }
        set(value) {
            field = value
            state["thingToDoNature"] = value
        }


    fun onSaveClick() {
        thingToDo?.let {
            val isSameNatureTTD = when (it) {
                is Task -> Nature.TASK == ttdNature
                is ProjectWithSubTasks -> Nature.PROJECT == ttdNature
                is Event -> Nature.EVENT == ttdNature
                else -> false
            }
            updateThingToDo(it, isSameNatureTTD)
        } ?: saveThingToDo()
    }


    private fun saveThingToDo() = viewModelScope.launch {
        when (ttdNature) {
            Nature.TASK -> {
                // TODO: 07/11/2022 take off possibility to change type of ttd if it's a sub task
                val newTask =
                    Task(
                        name,
                        priority.toInt(),
                        startDate,
                        deadline,
                        category,
                        description,
                        isRecurring = isRecurring,
                        recurringTaskInterval = recurringTaskInterval,
                        taskEstimatedTime = estimatedTime,
                        taskReminder = reminder,
                        taskEvaluationDescription = evaluationTaskDescription,
                        taskEvaluationGoal = evaluationTaskGoal,
                        taskEvaluationUnit = evaluationTaskUnit,
                        taskEvaluationDate = evaluationTaskDate
                    )
                if (projectId != null) {
                    repository.insertTask(newTask.copy(projectId = projectId))
                    val projectLinked = repository.getProjectData(projectId!!).project
                    repository.updateProject(projectLinked.copy(nb_sub_task = projectLinked.nb_sub_task + 1))
                } else {
                    repository.insertTask(newTask)
                }

                addEditChannelEvent.send(
                    AddEditTaskEvent.NavigateBackWithResult(
                        ADD_TASK_RESULT_OK
                    )
                )
            }
            Nature.PROJECT -> {
                repository.insertProject(
                    Project(
                        name,
                        priority.toInt(),
                        startDate,
                        deadline,
                        category,
                        description,
                        estimatedTime,
                        pjtReminder = reminder
                    )
                )
                addEditChannelEvent.send(
                    AddEditTaskEvent.NavigateBackWithResult(
                        ADD_PROJECT_RESULT_OK
                    )
                )
            }
            Nature.EVENT -> {
                repository.insertEvent(
                    Event(
                        name,
                        priority.toInt(),
                        startDate,
                        deadline,
                        description,
                        estimatedTime,
                        isSpread = eventIsSpread,
                        eventReminder = reminder
                    )
                )
                addEditChannelEvent.send(
                    AddEditTaskEvent.NavigateBackWithResult(
                        ADD_EVENT_RESULT_OK
                    )
                )
            }
        }
    }

    private fun updateThingToDo(thingToDo: ThingToDo, isSameNature: Boolean) =
        viewModelScope.launch {
            // TODO: 21/10/2022 update method to change a type of thing to do and for update an old item
            when (thingToDo) {
                is Task -> {
                    if (isSameNature) {
                        repository.updateTask(
                            thingToDo.copy(
                                name,
                                priority.toInt(),
                                startDate,
                                deadline,
                                category,
                                description,
                                isRecurring = isRecurring,
                                recurringTaskInterval = recurringTaskInterval,
                                taskEstimatedTime = estimatedTime,
                                taskReminder = reminder,
                                taskEvaluationDescription = evaluationTaskDescription,
                                taskEvaluationGoal = evaluationTaskGoal,
                                taskEvaluationUnit = evaluationTaskUnit,
                                taskEvaluationDate = evaluationTaskDate
                            )
                        )
                    } else {
                        repository.deleteTask(thingToDo)
                        saveThingToDo()
                    }
                    // TODO: 08/11/2022 change message for another type saved
                    addEditChannelEvent.send(
                        AddEditTaskEvent.NavigateBackWithResult(
                            EDIT_TASK_RESULT_OK
                        )
                    )
                }
                is ProjectWithSubTasks -> {
                    if (isSameNature) {
                        repository.updateProject(
                            thingToDo.project.copy(
                                pjtName = name,
                                pjtPriority = priority.toInt(),
                                pjtStartDate = startDate,
                                pjtCategory = category,
                                pjtDescription = description,
                                pjtDeadline = deadline,
                                pjtEstimatedTime = estimatedTime,
                                pjtReminder = reminder
                            )
                        )
                    } else {
                        repository.deleteProject(thingToDo.project)
                        saveThingToDo()
                    }
                    addEditChannelEvent.send(
                        AddEditTaskEvent.NavigateBackWithResult(
                            EDIT_PROJECT_RESULT_OK
                        )
                    )
                }
                is Event -> {
                    if (isSameNature) {
                        repository.updateEvent(
                            thingToDo.copy(
                                name,
                                priority.toInt(),
                                startDate,
                                deadline,
                                description,
                                estimatedTime,
                                isSpread = eventIsSpread,
                                eventReminder = reminder
                            )
                        )
                    } else {
                        repository.deleteEvent(thingToDo)
                        saveThingToDo()
                    }
                    addEditChannelEvent.send(
                        AddEditTaskEvent.NavigateBackWithResult(
                            EDIT_EVENT_RESULT_OK
                        )
                    )
                }
            }
        }

    fun showInvalidInputMessage(invalidUserMsg: String) = viewModelScope.launch {
        addEditChannelEvent.send(AddEditTaskEvent.ShowInvalidInputMessage(invalidUserMsg))
    }

    sealed class AddEditTaskEvent {
        data class ShowInvalidInputMessage(val msg: String) : AddEditTaskEvent()
        data class NavigateBackWithResult(val result: Int) : AddEditTaskEvent()
        //object NavigatePickerDateScreen : AddEditTaskEvent()
    }
}