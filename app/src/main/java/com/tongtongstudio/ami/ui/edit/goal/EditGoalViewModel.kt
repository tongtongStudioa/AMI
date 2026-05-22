package com.tongtongstudio.ami.ui.edit.goal

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.AssessmentType
import com.tongtongstudio.ami.domain.usecase.ScheduleAssessmentUseCase
import com.tongtongstudio.ami.ui.ADD_GOAL_RESULT_OK
import com.tongtongstudio.ami.ui.EDIT_GOAL_RESULT_OK
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditGoalViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle,
    private val scheduleAssessmentUseCase: ScheduleAssessmentUseCase
) : ViewModel() {


    private val editGoalEventChannel = Channel<EditGoalEvent>()
    val editGoalEvents = editGoalEventChannel.receiveAsFlow()
    val objective = state.get<Assessment>("global_goal")

    var goalTitle =
        state.get<String>("goalTitle") ?: objective?.title ?: ""
        set(value) {
            field = value
            state["goalTitle"] = value
        }
    var description =
        state.get<String>("description") ?: objective?.description
        set(value) {
            field = value
            state["description"] = value
        }
    var goal =
        state.get<String>("targetScore") ?: objective?.targetGoal.toString()
        set(value) {
            field = value
            state["targetGoal"] = value
        }

    var unit =
        state.get<String>("unit") ?: objective?.unit.toString()
        set(value) {
            field = value
            state["unit"] = value
        }

    var dueDate =
        state.get<Long>("dueDate") ?: objective?.dueDate
        set(value) {
            field = value
            state["dueDate"] = value
        }

    private val _assessments = MutableLiveData<MutableList<Assessment>>()
    val assessments: LiveData<MutableList<Assessment>>
        get() = _assessments

    init {
        // TODO: change repository method
        viewModelScope.launch {
            repository.getIntermediateAssessmentsByGoal(objective?.id)?.collect { assessments ->
                _assessments.value = assessments
            }
        }
    }

    private suspend fun updateAssessmentsList(parentId: Long): List<Assessment> {
        return if (assessments.value != null) {
            assessments.value!!.map {
                if (it.parentAssessmentId != parentId) {
                    val id = repository.insertAssessment( it.copy(parentId))
                    // update current assessment with no id (id = 0)
                    it.copy(id = id)
                } else it
            }
        } else emptyList()
    }



    fun addNewAssessment(result: Assessment) = viewModelScope.launch(Dispatchers.IO) {
        if (objective?.id == null) { // is a new global targetGoal ?
            val currentAssessments = assessments.value ?: mutableListOf()
            currentAssessments.add(result)
            _assessments.value = currentAssessments
        } else repository.insertAssessment( result.copy(objective.id))
    }

    fun updateAssessment(oldAssessment: Assessment, updateAssessment: Assessment) =
        viewModelScope.launch {
            if (oldAssessment.parentAssessmentId == null) {
                val currentReminders = _assessments.value ?: mutableListOf()
                val indexElement = currentReminders.indexOf(oldAssessment)
                currentReminders.remove(oldAssessment)
                currentReminders.add(indexElement, updateAssessment)
                _assessments.value = currentReminders
            } else
                repository.updateAssessment(updateAssessment)
        }

    fun removeAssessment(assessment: Assessment) = viewModelScope.launch {
        val updatedList: MutableList<Assessment> = _assessments.value ?: mutableListOf()
        updatedList.remove(assessment)
        _assessments.value = updatedList
        repository.deleteAssessment(assessment)
    }

    fun saveGlobalGoal() {
        if (objective != null)
            updateGlobalObjective()
        else insertGlobalObjective()
    }

    private fun insertGlobalObjective() = viewModelScope.launch(Dispatchers.IO) {
        val newObjective = Assessment(
            title = goalTitle,
            description = description,
            targetGoal = goal.toFloat(),
            unit = unit,
            dueDate = dueDate!!,
            // TODO: update assessment type
            type = AssessmentType.QUANTITY.name
        )
        val objectiveId = repository.insertAssessment(newObjective)
        val savedAssessments = updateAssessmentsList(objectiveId)
        scheduleAssessmentUseCase(savedAssessments)
        editGoalEventChannel.send(EditGoalEvent.NavigateBackWithResult(ADD_GOAL_RESULT_OK))
    }

    private fun updateGlobalObjective() = viewModelScope.launch(Dispatchers.IO) {
        val updatedObjective = objective!!.copy(
            title = goalTitle,
            description = description,
            targetGoal = goal.toFloat(),
            unit = unit,
            dueDate = dueDate!!
        )
        repository.updateAssessment(updatedObjective)
        val savedAssessments = updateAssessmentsList(objective.id)
        scheduleAssessmentUseCase(savedAssessments)
        editGoalEventChannel.send(EditGoalEvent.NavigateBackWithResult(EDIT_GOAL_RESULT_OK))
    }

    fun showInvalidInputMessage(invalidUserMsg: String) = viewModelScope.launch {
        editGoalEventChannel.send(EditGoalEvent.ShowInvalidInputMessage(invalidUserMsg))
    }

    sealed class EditGoalEvent {
        data class ShowInvalidInputMessage(val msg: String) : EditGoalEvent()
        data class NavigateBackWithResult(val result: Int) : EditGoalEvent()
    }
}
