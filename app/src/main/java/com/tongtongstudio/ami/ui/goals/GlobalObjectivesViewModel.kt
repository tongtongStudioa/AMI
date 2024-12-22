package com.tongtongstudio.ami.ui.goals

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Assessment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlobalObjectivesViewModel @Inject constructor(
    private val repository: Repository,
) : ViewModel() {

    private val goalEventChannel = Channel<GoalsEvent>()
    val goalsEvents = goalEventChannel.receiveAsFlow()

    fun deleteGoal(goal: Assessment) = viewModelScope.launch {
        repository.deleteAssessment(goal)
        goalEventChannel.send(GoalsEvent.ShowUndoDeleteGlobalGoalMessage(goal))
    }

    fun updateGoal(goal: Assessment) = viewModelScope.launch {
        goalEventChannel.send((GoalsEvent.NavigateToEditGlobalGoalScreen(goal)))
    }

    fun addGlobalGoal() = viewModelScope.launch {
        goalEventChannel.send(GoalsEvent.NavigateToAddGlobalGoalScreen)
    }

    fun onGoalClick(goal: Assessment, sharedView: View) = viewModelScope.launch {
        goalEventChannel.send(GoalsEvent.NavigateToDetailsGlobalGoalScreen(goal, sharedView))
    }

    fun onUndoDeleteClick(goal: Assessment) = viewModelScope.launch {
        repository.insertAssessment(goal.copy())
    }

    val globalGoals: LiveData<List<Assessment>> = repository.getGlobalGoals().asLiveData()

    sealed class GoalsEvent {
        data object NavigateToAddGlobalGoalScreen : GoalsEvent()
        data class NavigateToEditGlobalGoalScreen(val goal: Assessment) :
            GoalsEvent()

        data class NavigateToDetailsGlobalGoalScreen(val goal: Assessment, val sharedView: View) :
            GoalsEvent()
        data class ShowUndoDeleteGlobalGoalMessage(val goal: Assessment) :
            GoalsEvent()
    }
}