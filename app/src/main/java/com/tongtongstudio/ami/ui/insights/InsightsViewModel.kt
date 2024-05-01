package com.tongtongstudio.ami.ui.insights

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarEntry
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    val repository: Repository,
) : ViewModel() {

    private val _achievementsEntriesByPeriod = MutableLiveData<List<BarEntry>>()
    val achievementsEntriesByPeriod: LiveData<List<BarEntry>>
        get() = _achievementsEntriesByPeriod

    val _categoryId = MutableLiveData<Long?>(null)

    // TODO: use live data
    val categoryId: Long? = null
    val tasksAchievementRate =
        runBlocking { return@runBlocking repository.getTasksAchievementRate(categoryId) }
    val completedTasksCount =
        runBlocking { return@runBlocking repository.getMCompletedTasksCount(categoryId) }
    val projectsAchievementRate =
        runBlocking { return@runBlocking repository.getProjectsAchievementRate(categoryId) }
    val completedProjectsCount =
        runBlocking { return@runBlocking repository.getMCompletedProjectsCount(categoryId) }
    val timeWorked = runBlocking { return@runBlocking repository.getTimeWorked() }
    val ttdMaxStreak = runBlocking { return@runBlocking repository.getMaxStreak() }
    val ttdCurrentMaxStreak = runBlocking { return@runBlocking repository.getMaxStreak(true) }
    val accuracyRateEstimation =
        runBlocking { return@runBlocking repository.getAccuracyRateEstimation(categoryId) }
    val onTimeCompletionRate =
        runBlocking { return@runBlocking repository.getOnTimeCompletionRate(categoryId) }
    val habitCompletionRate =
        runBlocking { return@runBlocking repository.getHabitCompletionRate(categoryId) }

    val startDate: Long = Calendar.getInstance().run {
        add(Calendar.DAY_OF_MONTH, -7)
        timeInMillis
    }
    val endDate: Long = Calendar.getInstance().run {
        timeInMillis
    }

    fun updateAchievementEntries() {
        val achievementsEntries = getAchievementsByPeriod(startDate, endDate)
        _achievementsEntriesByPeriod.value = achievementsEntries
    }

    private fun getAchievementsByPeriod(startDate: Long, endDate: Long): List<BarEntry> {

        val arrayListAchievements = ArrayList<BarEntry>()
        val intermediateStartCalendar: Calendar = Calendar.getInstance()
        var startDay = intermediateStartCalendar.run {
            timeInMillis = startDate
            timeInMillis
        }
        var endDay = intermediateStartCalendar.run {
            add(Calendar.DAY_OF_MONTH, 1)
            timeInMillis
        }
        /*var i = 0.0F
        while (endDay < endDate) {
            val achievementsByDay = (Math.random() * 25 + 25)/*runBlocking {
                return@runBlocking repository.getMCompletedTasksCount(
                    categoryId,
                    startDay,
                    endDay
                )
            }*/
            i += 1.0F
            arrayListAchievements.add(BarEntry(i,achievementsByDay.toFloat()))
            startDay = intermediateStartCalendar.run {
                timeInMillis = endDay
                timeInMillis
            }
            endDay = intermediateStartCalendar.run {
                add(Calendar.DAY_OF_MONTH, 1)
                timeInMillis
            }
        }*/
        for (index in 0 until 7) {
            arrayListAchievements.add(BarEntry(0F, getRandom(25F, 25F)))
        }
        return arrayListAchievements.toList()
    }

    fun getRandom(range: Float, start: Float): Float {
        return (Math.random() * range).toFloat() + start
    }

    fun updateCategoryId(id: Long?) {
        _categoryId.value = id
    }
}
