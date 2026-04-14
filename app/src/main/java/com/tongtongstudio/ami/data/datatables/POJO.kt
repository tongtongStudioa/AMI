package com.tongtongstudio.ami.data.datatables

import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.ui.dialog.recurring_task.Period
import kotlinx.parcelize.Parcelize
import java.util.Calendar

const val PATTERN_FORMAT_DATE = "E dd/MM"
const val PATTERN_FORMAT_DATE_YEAR = "E dd/MM/yyyy"

enum class Nature { PROJECT, INTERMEDIATE_PROJECT, SUB_TASK, TASK }
enum class Type { UNIQUE, RECURRING }
enum class Status { NOT_STARTED, IN_PROGRESS, FINISHED, ABANDONED, REVIEW }

@Parcelize
data class TaskRelations(
    @Embedded
    val mainTask: Task,
    @Relation(
        parentColumn = "dependency_task_id",
        entityColumn = "task_id",
        entity = Task::class
    )
    val taskDependency: Task?, // The task on which the main task depends
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id",
        entity = Category::class
    )
    val category: Category?,
    @Relation(
        parentColumn = "parent_task_id",
        entityColumn = "task_id",
        entity = Task::class
    )
    val parentProject: Task?
) : Parcelable

@Parcelize
data class ThingToDo(
    @Embedded
    val taskRelations: TaskRelations,
    @ColumnInfo(name = "total_main_sub_ttd")
    val totalMainSubTtd: Int?,
    @ColumnInfo(name = "nb_sub_tasks")
    val nbSubTasks: Int?,
    @ColumnInfo(name = "nb_sub_tasks_completed")
    val nbSubTasksCompleted: Int?,
    @ColumnInfo(name = "last_completion_status")
    val lastCompletionStatus: Boolean?, // True if completed already or False or null if not
    @ColumnInfo(name = "completion_rate")
    val completionRate: Float?
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
     * This function return a string resource to display nature of the task.
     * The Nature of the task describe how it will be show to the user
     * and how it will be treated.
     */
    fun getNature(context: Context): String {
        return when (taskRelations.mainTask.nature) {
            Nature.PROJECT.name -> context.getString(R.string.project)
            Nature.INTERMEDIATE_PROJECT.name -> context.getString(R.string.intermediate_project)
            Nature.SUB_TASK.name -> context.getString(R.string.sub_task)
            else -> context.getString(R.string.task)
        }
    }

    fun getStatus(context: Context): String { // TODO: reflect on system to take in account working time progress and other possibilities (review and abandoned status)
        return when (taskRelations.mainTask.status) {
            Status.NOT_STARTED.name -> context.getString(R.string.not_started)
            Status.IN_PROGRESS.name -> context.getString(R.string.in_progress)
            Status.REVIEW.name -> context.getString(R.string.on_review)
            Status.FINISHED.name -> context.getString(R.string.finished)
            Status.ABANDONED.name -> context.getString(R.string.abondoned)
            else -> "problem"
        }
    }

    fun isLate(): Boolean {
        val todayDate = Calendar.getInstance().run{
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            timeInMillis
        }
        return !taskRelations.mainTask.isDraft && (lastCompletionStatus == false || lastCompletionStatus == null) && (taskRelations.mainTask.dueDate ?: 0) < todayDate
    }

    fun getAdvancementStatus(): String {
        return "$nbSubTasksCompleted/$nbSubTasks"
    }

    fun getPercentageProgress(): Float {
        if (nbSubTasks == null || nbSubTasks == 0)
            return 0.toFloat()
        val progress: Float = nbSubTasksCompleted!!.toFloat() / nbSubTasks * 100
        return progress
    }
}

/**
 * Details class for Thing To Do to display complementary information in monitoring view.
 */
data class ThingToDoDetails(
    @Embedded
    val taskRelations: TaskRelations,
    @Relation(
        parentColumn = "task_id",
        entityColumn = "parent_task_id",
        entity = TaskCompletion::class
    )
    val completions: List<TaskCompletion>,
    @Relation(parentColumn = "task_id", entityColumn = "parentTaskId", entity = WorkSession::class)
    val workSessions: List<WorkSession>,
    @Relation(
        parentColumn = "task_recurrence_id",
        entityColumn = "recurrence_id",
        entity = TaskRecurrence::class
    )
    val taskRecurrence: TaskRecurrence?, // recurrence details
    @Relation(
        parentColumn = "task_recurrence_id",
        entityColumn = "day_id",
        associateBy = Junction(
            TaskRecurrenceDaysCrossRef::class,
            parentColumn = "recurrenceId",
            entityColumn = "dayId"
        )
    )
    val daysOfWeek: List<DaysOfWeek>?  // days associate
)

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
    val title: String? = "No task",
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
        associateBy = Junction(
            TaskRecurrenceDaysCrossRef::class,
            parentColumn = "recurrenceId",
            entityColumn = "dayId"
        )
    )
    val daysOfWeek: List<DaysOfWeek> // days associate
) : Parcelable {
    /**
     * Update recurring task depending with task's recurrence characteristics (delay, repetition frequency, etc.)
     * @param oldDueDate : old task due date
     * @return new due date
     */
    fun getNextOccurrenceDay(oldDueDate: Long): Long {
        val newDueDate = if (daysOfWeek.isNotEmpty()) {
            findNextOccurrenceDayInWeek(oldDueDate)
        } else {
            findNextOccurrenceDay(oldDueDate)
        }
        return newDueDate
    }

    fun findFirstNextOccurrenceDayInWeek(oldDueDate: Long): Long {
        val newStartDate = Calendar.getInstance().run {
            timeInMillis = oldDueDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            do {
                add(Calendar.DAY_OF_WEEK, 1)
                val nextDay = get(Calendar.DAY_OF_WEEK)

                // if we change of week so add interval week if interval > 2 week
                if (get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                    add(Calendar.DAY_OF_MONTH, (taskRecurrence.interval - 1) * 7)
                }
            } while (!daysOfWeek.any { it.dayId == nextDay })
            timeInMillis
        }
        return newStartDate
    }

    private fun findNextOccurrenceDayInWeek(oldDueDate: Long): Long {
        val newDueDate = Calendar.getInstance().run {
            val todayDateInMillis = timeInMillis
            timeInMillis = oldDueDate
            do {
                timeInMillis = findFirstNextOccurrenceDayInWeek(timeInMillis)
                Log.e("Find next occurrence", timeInMillis.toString())
            } while (timeInMillis < todayDateInMillis)
            // if list of recurrence days contains next deadline's day and it's after  today : set a new due date
            timeInMillis
        }
        return newDueDate
    }

    private fun findFirstNextOccurrenceDay(oldDueDate: Long): Long {
        val newStartDate = Calendar.getInstance().run {
            timeInMillis = oldDueDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            when (taskRecurrence.frequency) {
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

                else -> add(Calendar.DAY_OF_MONTH, 1)
            }
            timeInMillis
        }
        return newStartDate
    }

    private fun findNextOccurrenceDay(oldDueDate: Long): Long {
        val newStartDate = Calendar.getInstance().run {
            val todayTimeInMillis = timeInMillis
            timeInMillis = oldDueDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Set the new due date to the next occurrence of the task's due day
            do {
                 timeInMillis = findFirstNextOccurrenceDay(timeInMillis)
            } while (timeInMillis < todayTimeInMillis)
            timeInMillis
        }
        return newStartDate
    }

    fun calculateAllDueDatesBetween(
        startDate: Long,
        endDate: Long,
    ): List<Long> {
        val dueDates = mutableListOf<Long>()

        var currentDueDate = startDate

        while (currentDueDate < endDate) {
            dueDates.add(currentDueDate)
            currentDueDate = if (daysOfWeek.isNotEmpty())
                findFirstNextOccurrenceDayInWeek(currentDueDate)
            else findFirstNextOccurrenceDay(currentDueDate)
            Log.e("Calculate all dates", currentDueDate.toString())
        }
        Log.e("Calculate all dates", dueDates.toString())
        return dueDates.sorted()
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

    fun getRecurringIntervalReadable(resources: Resources): String {
        return if (taskRecurrence.interval == 1 && daysOfWeek.isEmpty()) {
            when (taskRecurrence.frequency) {
                Period.DAYS.name -> resources.getString(R.string.each_days)
                Period.WEEKS.name -> resources.getString(R.string.each_weeks)
                Period.MONTHS.name -> resources.getString(R.string.each_months)
                Period.YEARS.name -> resources.getString(R.string.each_years)
                else -> resources.getString(R.string.each_days)
            }
        } else if (daysOfWeek.isNotEmpty()) {
            // TODO: create function to retrieve E from int : Mon, Tue, Wed, Thu, Fri (Lun, Mar, Mer, Jeu, Ven, ...)
            if (taskRecurrence.interval == 1) resources.getString(
                R.string.weekly_interval,
                daysOfWeek.joinToString(",") { it.name }
            )
            else "On ${daysOfWeek.joinToString(",") { it.name }} every ${taskRecurrence.interval} weeks"
        } else {
            when (taskRecurrence.frequency) {
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

data class ReminderNotification(
    @ColumnInfo(name = "reminder_id")
    val reminderId: Long,
    val dueDate: Long,
    val taskTitle: String
)

data class RepeatProcess(val newDueDate: Long, val timesSkipped: Int = 0)

data class IndicatorRateByPeriod(val period: String, val rate: Float)

data class CountSinceLastCompletion(val lastCompletionDate: String, val missedCount: Int)