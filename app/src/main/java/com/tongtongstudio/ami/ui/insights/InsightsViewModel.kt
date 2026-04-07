package com.tongtongstudio.ami.ui.insights

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    val repository: Repository,
) : ViewModel() {

    private val _uiState: MutableStateFlow<InsightsUiState> = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState>
        get() = _uiState

    val startDate: Long = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        add(Calendar.DAY_OF_MONTH, -7)
        timeInMillis
    }

    val endDate: Long = Calendar.getInstance().run {
        timeInMillis
    }

    private val _categoryId = MutableLiveData<Long?>(null)
    val categoryId: LiveData<Long?>
        get() = _categoryId

    init {
        viewModelScope.launch {
            repository.getCategories().collect { categories ->
                _uiState.update {
                    it.copy(categories = categories)
                }
            }
        }
        loadInsights()
    }

    fun loadInsights(categoryId: Long? = null) = viewModelScope.launch(Dispatchers.IO) {

        val tasksAchievementRate =  repository.getTasksAchievementRate(categoryId)
        val completedTasksCount = repository.getCompletedTasksCount(categoryId)
        val projectsAchievementRate = repository.getProjectsAchievementRate(categoryId)
        val completedProjectsCount = repository.getCompletedProjectsCount(categoryId)
        val timeWorked = repository.getTimeWorked(categoryId)
        val ttdMaxStreak =
            repository.getMaxStreak(categoryId)
        val ttdCurrentMaxStreak =
            repository.getCurrentMaxStreak(categoryId)
        val accuracyRateEstimation =
            repository.getAccuracyRateEstimation(categoryId)
        val onTimeCompletionRate =
            repository.getOnTimeCompletionRate(categoryId)
        val habitCompletionRate = repository.getHabitCompletionRate(categoryId)
        val timeWorkedDistribution =  repository.getTimeWorkedGrouped(categoryId)

        _uiState.update {
            it.copy(
                tasksAchievementRate = tasksAchievementRate,
                completedTasksCount = completedTasksCount,
                completedProjectsCount = completedProjectsCount,
                projectsAchievementRate = projectsAchievementRate,
                timeWorked = timeWorked,
                ttdMaxStreak = ttdMaxStreak,
                ttdCurrentMaxStreak = ttdCurrentMaxStreak,
                accuracyRateEstimation = accuracyRateEstimation,
                onTimeCompletionRate = onTimeCompletionRate,
                habitCompletionRate = habitCompletionRate,
                timeWorkedDistribution = timeWorkedDistribution
            )
        }
    }
    fun updateCategoryId(title: String) = viewModelScope.launch {
        val category = repository.getCategoryByTitle(title)
        _categoryId.value = category?.id
    }

    fun updateCategoryId(id: Long) = viewModelScope.launch {
        val category = repository.getCategoryById(id)
        _categoryId.value = category.id
    }
}
