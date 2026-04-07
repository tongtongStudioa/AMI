package com.tongtongstudio.ami.ui.insights

import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.TimeWorkedDistribution
import com.tongtongstudio.ami.data.datatables.TtdAchieved
import com.tongtongstudio.ami.data.datatables.TtdStreakInfo

data class InsightsUiState(
    val categories: List<Category> = emptyList(),
    val categoryId: Long? = null,
    val tasksAchievementRate: Float = 0f,
    val completedTasksCount: Int = 0,
    val projectsAchievementRate: Float =0f,
    val completedProjectsCount: Int =0,
    val timeWorked: Long = 0L,
    val ttdMaxStreak: TtdStreakInfo? = null,
    val ttdCurrentMaxStreak: TtdStreakInfo? = null,
    val accuracyRateEstimation: Float? = 0F,
    val onTimeCompletionRate: Float? =0f,
    val habitCompletionRate: Float? =0f,
    val timeWorkedDistribution: List<TimeWorkedDistribution> = emptyList(),
    val achievementsByPeriod: List<TtdAchieved?> = emptyList()
)