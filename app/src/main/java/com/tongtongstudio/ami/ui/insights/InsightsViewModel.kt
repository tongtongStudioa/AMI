package com.tongtongstudio.ami.ui.insights

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    val repository: Repository,
) : ViewModel() {

    private val _categoryId = MutableLiveData<Long?>(null)
    val categoryId: LiveData<Long?>
        get() = _categoryId

    val tasksAchievementRate =
        categoryId.asFlow().flatMapLatest { repository.getTasksAchievementRate(it) }.asLiveData()
    val completedTasksCount =
        categoryId.asFlow().flatMapLatest { repository.getCompletedTasksCount(it) }.asLiveData()
    val projectsAchievementRate =
        categoryId.asFlow().flatMapLatest { repository.getProjectsAchievementRate(it) }.asLiveData()
    val completedProjectsCount =
        categoryId.asFlow().flatMapLatest { repository.getCompletedProjectsCount(it) }.asLiveData()
    val timeWorked = categoryId.asFlow().flatMapLatest { repository.getTimeWorked(it) }.asLiveData()
    val ttdMaxStreak =
        categoryId.asFlow().flatMapLatest { repository.getMaxStreak(it) }.asLiveData()
    val ttdCurrentMaxStreak =
        categoryId.asFlow().flatMapLatest { repository.getCurrentMaxStreak(it) }.asLiveData()
    val accuracyRateEstimation =
        categoryId.asFlow().flatMapLatest { repository.getAccuracyRateEstimation(it) }.asLiveData()
    val onTimeCompletionRate =
        categoryId.asFlow().flatMapLatest { repository.getOnTimeCompletionRate(it) }.asLiveData()
    val habitCompletionRate =
        categoryId.asFlow().flatMapLatest { repository.getHabitCompletionRate(it) }.asLiveData()
    val timeWorkedDistribution =
        categoryId.asFlow().flatMapLatest { repository.getTimeWorkedGrouped(it) }.asLiveData()

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

    val achievementsByPeriod =
        categoryId.asFlow()
            .flatMapLatest { repository.getCompletedTasksCountByPeriod(it, startDate, endDate) }
            .asLiveData()

    fun updateCategoryId(title: String) = viewModelScope.launch {
        val category = repository.getCategoryByTitle(title)
        _categoryId.value = category?.id
    }

    val categories = repository.getCategories().asLiveData()
}
