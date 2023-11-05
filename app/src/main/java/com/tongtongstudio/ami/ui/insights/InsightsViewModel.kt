package com.tongtongstudio.ami.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Project
import com.tongtongstudio.ami.data.datatables.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repository: Repository,
) : ViewModel() {
    val tasksCompletedLD = repository.getTasksCompletedStats().asLiveData()
    val projectsLD = repository.getProjectCompletedStats().asLiveData()

    fun getAverageTimeCompletion(tasksCompleted: List<Task>): Long? {
        var sum = 0L
        var taskNoCount = 0
        tasksCompleted.forEach {
            sum += if (it.taskWorkTime != null && it.taskWorkTime != 0L)
                it.taskWorkTime
            else {
                taskNoCount += 1
                0L
            }
        }
        val averageTimeCompletion: Long? =
            if (tasksCompleted.isEmpty() || (taskNoCount - tasksCompleted.size) == 0)
                null
            else
                (sum.toFloat() / (tasksCompleted.size - taskNoCount)).toLong()
        return averageTimeCompletion
    }

    fun getProjectAchievementRate(projectsList: List<Project>): Float? {
        var sumSubTasks = 0
        var sumSubTasksCompleted = 0
        projectsList.forEach {
            sumSubTasks += it.nb_sub_task
            sumSubTasksCompleted += it.nb_sub_tasks_completed
        }
        return if (sumSubTasks == 0) null else sumSubTasksCompleted / sumSubTasks.toFloat() * 100
    }

    fun retrieveEstimationWorkTimeAccuracyRate(tasksCompleted: List<Task>): Float? {
        var tasksStudied = 0
        var rateSum = 0F
        tasksCompleted.forEach {
            val completionTime = it.taskWorkTime
            val estimatedTimeInMillis = it.taskEstimatedTime //in minutes

            if (completionTime != null && estimatedTimeInMillis != null) {
                val difference = abs((completionTime - estimatedTimeInMillis))
                rateSum += if (difference < estimatedTimeInMillis) {
                    (1 - difference / estimatedTimeInMillis.toFloat())
                } else {
                    0F
                }
                tasksStudied += 1
            }
        }
        return if (tasksCompleted.isEmpty() || tasksStudied == 0) null else rateSum / tasksStudied * 100
    }

    fun retrieveOnTimeCompletionRate(tasksCompleted: List<Task>): Float? {
        val calendar = Calendar.getInstance()
        var nbCompletedOnTime = 0
        var tasksNoStudied = 0
        tasksCompleted.forEach { task ->
            if (!task.isRecurring) { // no count of recurring task (maybe later)
                val completedDateInMillis = task.taskCompletedDate!!
                val completedDate = calendar.run {
                    timeInMillis = task.taskCompletedDate
                    time
                }
                // task's deadline mustn't be null !! --> edit fragment: save method
                if (task.deadline == null) {
                    tasksNoStudied++
                } else {
                    val deadlineDate = calendar.run {
                        timeInMillis = task.deadline
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        time
                    }
                    val wasCompletedOnTime: Boolean =
                        completedDate <= deadlineDate || completedDateInMillis < task.deadline
                    if (wasCompletedOnTime) nbCompletedOnTime++
                }
            } else tasksNoStudied++
        }
        return if (tasksCompleted.isEmpty() || tasksNoStudied == tasksCompleted.size) null else nbCompletedOnTime / tasksCompleted.size.toFloat() * 100
    }

    fun getProjectsCompleted(projectsList: List<Project>): Int {
        val projectCompleted = ArrayList<Project>()
        projectsList.forEach {
            if (it.isPjtCompleted) projectCompleted.add(it)
        }
        return projectCompleted.toList().size
    }
}
