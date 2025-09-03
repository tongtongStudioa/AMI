package com.tongtongstudio.ami.data.datatables

import android.content.res.Resources
import android.os.Parcelable
import android.util.Log
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Junction
import androidx.room.Relation
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.ui.dialog.Period
import kotlinx.parcelize.Parcelize
import java.util.Calendar

const val PATTERN_FORMAT_DATE = "E dd/MM"
const val PATTERN_FORMAT_DATE_YEAR = "E dd/MM/yyyy"
enum class Nature { PROJECT, INTERMEDIATE_PROJECT, SUB_TASK, TASK }
enum class Type { UNIQUE, RECURRING }
enum class Status { NOT_STARTED, IN_PROGRESS, FINISHED, ABANDONED, REVIEW}

@Parcelize
data class TaskRelations(
    @Embedded
    val mainTask: Task,
    @Relation(
        parentColumn = "dependency_task_id",
        entityColumn = "task_id",
        entity = Task::class)
    val taskDependency: Task?, // The task on which the main task depends
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id",
        entity = Category::class)
    val category: Category?,
    @Relation(
        parentColumn = "parent_task_id",
        entityColumn = "task_id",
        entity = Task::class)
    val parentProject: Task?
) : Parcelable

@Parcelize
data class ThingToDo(
    @Embedded
    val taskRelations: TaskRelations,
    val nbSubTasks: Int?,
    val nbSubTasksCompleted: Int?,
    val lastCompletionStatus: Boolean? // True if completed already or False if not
) : Parcelable {

    /**
     * The type describe the particularity of a task to be recurring or not.
     */
    fun getType(): String {
        val type: Type =
            if (taskRelations.mainTask.recurrenceInfosId == null) Type.UNIQUE else Type.RECURRING
        return type.name
    }

    /**
     * The Nature of the task describe how it will be show to the user
     * and how it will be treated.
     */
    fun getNature(): String {
        val nature: Nature = if (taskRelations.parentProject!= null && nbSubTasks != null && nbSubTasks > 0 || taskRelations.mainTask.nature == Nature.INTERMEDIATE_PROJECT.name)
            Nature.INTERMEDIATE_PROJECT
        else if ((nbSubTasks?.let { it > 0 } == true) || taskRelations.mainTask.nature == Nature.PROJECT.name)
            Nature.PROJECT
        else if (taskRelations.parentProject != null)
            Nature.SUB_TASK
        else Nature.TASK
        return nature.name
    }

    fun getStatus(): String {
        return if (lastCompletionStatus != null) Status.IN_PROGRESS.name else Status.NOT_STARTED.name
    }

    /*fun countCompletedSubtasks(): Int {
        // Count direct sub tasks using last completion
        val directCompleted = subTasks.count { subtask ->
            subtask.completions.lastOrNull()?.isCompleted == true
        }
        // Add embedded completed sub tasks
        val nestedCompleted = subTasks.sumOf { it.countCompletedSubtasks() }
        return directCompleted + nestedCompleted
    }*/
    fun getAdvancementStatus(): String {
        return "$nbSubTasksCompleted/$nbSubTasks"
    }

    fun getPercentageProgress(): Float? {
        if (nbSubTasks == null)
            return null
        val progress: Float = nbSubTasksCompleted!!.toFloat() / nbSubTasks * 100
        return progress
    }
}
/**
 * Class with task completed info to analyse productivity (number of task achieved in a period of time).
 */
data class TtdAchieved(
    val completionDate: Long,
    val completedCount: Float
)

/**
 * Class with recurring task info for max and min streak.
 */
data class TtdStreakInfo(
    val title: String,
    val streak: Int,
)

/**
 * Describe type of objective targetGoal to help tracking progress.
 */
enum class AssessmentType { QUANTITY, DURATION, BOOLEAN }

data class TimeWorkedDistribution(val title: String?, val totalTimeWorked: Long?)

data class CategoryTasks(
    @Embedded
    val category: Category,
    @Relation(entity = Task::class, parentColumn = "category_id", entityColumn = "category_id")
    val tasks: List<Task>
)

@Parcelize
data class TaskRecurrenceWithDays(
    @Embedded val taskRecurrence: TaskRecurrence, // recurrence details
    @Relation(
        parentColumn = "recurrence_id",
        entityColumn = "day_id",
        associateBy = Junction(TaskRecurrenceDaysCrossRef::class,
            parentColumn = "recurrenceId",
            entityColumn = "dayId")
    )
    val daysOfWeek: List<DaysOfWeek> // days associate
) : Parcelable
{
    /**
     * Update recurring task depending with task's recurrence characteristics (delay, repetition frequency, etc.)
     * @param oldDueDate : old task due date
     * @param checked : checked state for automatic update
     * @return new due date
     */
    fun findNextDueDate(oldDueDate: Long, checked: Boolean): Long {
        val updatedStartDate = if (daysOfWeek.isNotEmpty()) {
            findNextOccurrenceDayInWeek(oldDueDate, checked)
        } else {
            findNextOccurrenceDay(oldDueDate, checked)
        }
        val newDueDateDate = updatedStartDate.newDueDate
        val timesSkipped = updatedStartDate.timesSkipped

        return newDueDateDate
    }

    private fun findNextOccurrenceDayInWeek(oldDueDate: Long, checked: Boolean): RepeatProcess {
        var timesSkipped = 0
        val newStartDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val todayDateInMillis = timeInMillis
            timeInMillis = oldDueDate
            do {
                // manage skipped if it contains the day and it's not checked
                if (daysOfWeek.any { it.dayId == get(Calendar.DAY_OF_WEEK) } && !checked)
                    timesSkipped++

                add(Calendar.DAY_OF_WEEK, 1)
                val nextDay = get(Calendar.DAY_OF_WEEK)

                // if we change of week so add intervalWeek if interval > 2 week (times = num of week interval)
                if (get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                    add(Calendar.DAY_OF_MONTH, (taskRecurrence.interval - 1) * 7)
                }
                val nextDueDate = timeInMillis
                val notContainsAndBeforeToday =
                    !(daysOfWeek.any { it.dayId == nextDay } && nextDueDate >= todayDateInMillis)
            } while (notContainsAndBeforeToday)
            // if list of recurrence days contains next deadline's day and it's after  today : set a new due date
            timeInMillis
        }
        return RepeatProcess(newStartDate, timesSkipped)
    }

    private fun findNextOccurrenceDay(oldDueDate: Long, checked: Boolean): RepeatProcess {
        var timesSkipped = 0
        var todayTimeInMillis: Long
        val newStartDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            todayTimeInMillis = timeInMillis
            // Set the new due date to the next occurrence of the task's due day
            timeInMillis = oldDueDate
            do {
                when (taskRecurrence.frequency.lowercase()) {
                    Period.DAYS.name -> add(
                        Calendar.DAY_OF_MONTH,
                        taskRecurrence.interval * 1
                    )

                    Period.WEEKS.name -> add(
                        Calendar.DAY_OF_MONTH,
                        taskRecurrence.interval * 7
                    )

                    Period.MONTHS.name -> add(
                        Calendar.MONTH,
                        taskRecurrence.interval * 1
                    )

                    Period.YEARS.name -> add(
                        Calendar.YEAR,
                        taskRecurrence.interval * 1
                    )

                    else -> add(Calendar.DAY_OF_MONTH, 0)
                }
                if (!checked)
                    timesSkipped++
            } while (timeInMillis < todayTimeInMillis)
            timeInMillis
        }
        return RepeatProcess(newStartDate, timesSkipped)
    }

    fun setStartDateSpecificDay(): Long {
        return if (daysOfWeek.isNotEmpty()) {
            val startDate = Calendar.getInstance().run {
                while (get(Calendar.DAY_OF_WEEK) != daysOfWeek.first().dayId) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                timeInMillis
            }
            startDate
        } else Calendar.getInstance().timeInMillis
    }

    fun getNextOccurrenceDay(oldDueDate: Long, validate: Boolean): Long {
        val repeatProcess = if (daysOfWeek.isNotEmpty()) {
            findNextOccurrenceDayInWeek(oldDueDate, validate)
        } else {
            findNextOccurrenceDay(oldDueDate, validate)
        }
        return repeatProcess.newDueDate
    }

    fun getRecurringIntervalReadable(resources: Resources): String {
        return if (taskRecurrence.interval == 1 && daysOfWeek.isEmpty()) {
            when (taskRecurrence.frequency.lowercase()) {
                Period.DAYS.name -> resources.getString(R.string.each_days)
                Period.WEEKS.name -> resources.getString(R.string.each_weeks)
                Period.MONTHS.name -> resources.getString(R.string.each_months)
                Period.YEARS.name -> resources.getString(R.string.each_years)
                else -> resources.getString(R.string.each_days)
            }
        } else if (daysOfWeek.isNotEmpty()) {
            // TODO: create function to retrieve E from int : Mon, Tue, Wed, Thu, Fri (Lun, Mar, Mer, Jeu, Ven, ...)
            if (daysOfWeek.size == 1) resources.getString(
                R.string.weekly_interval,
                daysOfWeek[0].name
            )
            else "On $daysOfWeek every ${taskRecurrence.interval} weeks"
        } else {
            when (taskRecurrence.frequency.lowercase()) {
                Period.DAYS.name -> resources.getString(
                    R.string.every_x_days,
                    taskRecurrence.interval
                )

                Period.WEEKS.name -> resources.getString(
                    R.string.every_x_weeks,
                    taskRecurrence.interval
                )

                Period.MONTHS.name -> resources.getString(
                    R.string.every_x_months,
                    taskRecurrence.interval
                )

                Period.YEARS.name -> resources.getString(
                    R.string.every_x_years,
                    taskRecurrence.interval
                )

                else -> resources.getString(R.string.every_x_days, taskRecurrence.interval)
            }
        }
    }
}

class RepeatProcess(val newDueDate: Long, val timesSkipped: Int = 0)

data class IndicatorRateByPeriod(val period: String, val rate: Float)

data class CountSinceLastCompletion(val lastCompletionDate: String, val missedCount: Int)