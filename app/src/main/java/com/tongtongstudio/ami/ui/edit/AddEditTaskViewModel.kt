package com.tongtongstudio.ami.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.RecurringTaskInterval
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Ttd
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

    // TODO: get reminder of the tasks : DAO method
    /*var reminder: Long? =
        state.get<Long>("reminder") ?: thingToDo?.
        set(value) {
            field = value
            state["reminder"] = value
        }
     */

    var startDate =
        state.get<Long>("thingToDoStartDate") ?: thingToDo?.startDate
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

    var recurringTaskInterval =
        state.get<RecurringTaskInterval>("recurringTaskInterval") ?: thingToDo?.repetitionFrequency
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

    fun onSaveClick() {
        thingToDo?.let {
            updateThingToDo(it)
        } ?: saveThingToDo()
    }

    fun getCategoryId(category: Category): Long {
        return category.id
    }

    fun getAssesments(): List<Assessment>? = runBlocking {
        return@runBlocking if (thingToDo != null) {
            repository.getTasksAssessments(thingToDo.id).first()
        } else null
    }

    fun addAssessment(taskId: Long, newAssessment: Assessment) = viewModelScope.launch {
        repository.insertAssessment(newAssessment.copy(taskId = taskId))
    }

    private fun saveThingToDo() = viewModelScope.launch {
        // TODO: Add all attributes like : dueDate, dependency,skillLevel, importance, urgency  , categoryId and add reminder
        val newThingToDo =
            Ttd(
                title,
                priority.toInt(),
                dueDate = deadline!!,
                startDate,
                deadline,
                Nature.TASK.name,
                description,
                estimatedTime = estimatedTime,
                isRecurring = isRecurring,
                repetitionFrequency = recurringTaskInterval,
                parentTaskId = projectId
            )

        repository.insertTask(newThingToDo)
        addEditChannelEvent.send(
            AddEditTaskEvent.NavigateBackWithResult(
                ADD_TASK_RESULT_OK
            )
        )
    }

    private fun updateThingToDo(thingToDo: Ttd) =
        viewModelScope.launch {
            // TODO: 21/10/2022 update method to change a type of thing to do and for update an old item
            repository.updateTask(
                thingToDo.copy(
                    title = title,
                    priority = priority.toInt(),
                    dueDate = deadline!!,
                    startDate = startDate,
                    deadline = deadline,
                    description = Nature.TASK.name,
                    type = description,
                    estimatedTime = estimatedTime,
                    isRecurring = isRecurring,
                    repetitionFrequency = recurringTaskInterval,
                    parentTaskId = projectId
                )
            )
            addEditChannelEvent.send(
                AddEditTaskEvent.NavigateBackWithResult(
                    EDIT_TASK_RESULT_OK
                )
            )
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