package com.tongtongstudio.ami.ui.monitoring.goal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class GoalDetailsViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle
) : ViewModel() {
    val goal: Assessment? = state.get<Assessment>("goal")

    val category: Category? = getGoalCategory()

    val intermediateEvaluations = repository.getIntermediateAssessmentsByGoal(goal?.id)?.asLiveData()

    private fun getGoalCategory(): Category? = runBlocking {
        if (goal?.categoryId != null)
            return@runBlocking repository.getCategoryById(goal.categoryId)
        else null
    }

    fun deleteIntermediateAssessment(attribute: Assessment) = viewModelScope.launch {
        repository.deleteAssessment(attribute)
    }
}
