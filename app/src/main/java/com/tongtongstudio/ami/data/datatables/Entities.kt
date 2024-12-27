package com.tongtongstudio.ami.data.datatables

import android.content.res.Resources
import android.os.Parcelable
import android.text.format.DateUtils.DAY_IN_MILLIS
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.ui.dialog.Period
import kotlinx.parcelize.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.pow

const val PATTERN_FORMAT_DATE = "E dd/MM"

enum class Nature { PROJECT, TASK }

@Parcelize
@Entity(tableName = "days_of_week_table")
data class DaysOfWeek(
    @PrimaryKey(autoGenerate = false) // days are fixed
    val dayId: Int, // 1 (Monday) to 7 (Sunday)
    val name: String // "Monday", "Tuesday", etc.
) : Parcelable

@Parcelize
@Entity(tableName = "task_recurrence_table")
data class TaskRecurrence(
    val frequency: String, // e.g., "DAILY", "WEEKLY", "MONTHLY", etc.
    val interval: Int, // e.g., 1 (every 1 day), 2 (every 2 weeks), etc.
    @ColumnInfo(name = "start_date")
    val startDate: Long?, // Timestamp when the recurrence starts
    @ColumnInfo(name = "end_date")
    val endDate: Long? = null, // Timestamp when the recurrence ends
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true, // Whether the recurrence is currently active
    @ColumnInfo(name = "occurrence_limit")
    val occurrenceLimit: Int? = null, // Number of occurrences before the recurrence stops
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "recurrence_id")
    val recurrenceId: Long = 0
) : Parcelable {

    /**
     * Create a new interval with user's feedback to increase or decrease last recurring interval.
     * @param userFeedback
     * @return TaskRecurrence
     */
    fun createNewInterval(userFeedback: Boolean): TaskRecurrence {
        return if (userFeedback) increaseInterval() else decreaseInterval()
    }

    private fun increaseInterval(): TaskRecurrence {
        // TODO: find a correct way to increase interval
        val newInterval = interval.toDouble().pow(2).toInt()
        return this.copy(interval = newInterval)
    }

    private fun decreaseInterval(): TaskRecurrence {
        val newInterval = if (interval > 1) interval - 1 else interval
        return this.copy(interval = newInterval)
    }
}

@Entity(
    tableName = "task_recurrence_days_cross_ref",
    primaryKeys = ["recurrenceId", "dayId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskRecurrence::class,
            parentColumns = ["recurrence_id"],
            childColumns = ["recurrenceId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = DaysOfWeek::class,
            parentColumns = ["dayId"],
            childColumns = ["dayId"],
            onDelete = CASCADE
        )
    ]
)
data class TaskRecurrenceDaysCrossRef(
    val recurrenceId: Long,
    val dayId: Int
)

@Parcelize
@Entity(
    tableName = "task_completion_table", foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["task_id"],
            childColumns = ["parent_task_id"],
            onDelete = CASCADE
        )
    ]
)
data class Completion(
    @ColumnInfo(name = "parent_task_id")
    val taskId: Long,
    val isCompleted: Boolean = false,
    val completionDate: Long? = null,
    val comment: String? = null,
    val emotions: Int? = 1,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "completion_id")
    val id: Long = 0
) : Parcelable {
    /**
     * This function decide if a thingToDo is completed on time or not.
     * It compares completionDate and dueDate or completionDate and deadline if it was define
     * @return boolean
     */
    private fun hasBeenCompletedOnTime(
        completionDate: Long,
        dueDate: Long,
        deadline: Long?
    ): Boolean {
        return completionDate < dueDate || (deadline != null && completionDate < deadline)
    }

    fun getCompletionDateFormatted(): String {
        return DateFormat.getDateInstance().format(completionDate)
    }
}

@Parcelize
@Entity(
    tableName = "task_table", foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["category_id"],
            childColumns = ["categoryId"],
            onDelete = SET_NULL
        ), ForeignKey(
            entity = Task::class,
            parentColumns = ["task_id"],
            childColumns = ["parent_task_id"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = TaskRecurrence::class,
            parentColumns = ["recurrence_id"],
            childColumns = ["task_recurrence_id"],
            onDelete = SET_NULL
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["task_id"],
            childColumns = ["dependency_task_id"],
            onDelete = SET_NULL
        )]
)
data class Task(
    val title: String,
    val priority: Int?,
    @ColumnInfo(name = "task_due_date")
    val dueDate: Long?, // when the thingToDo must be complete (to get ahead of the deadline)
    val startDate: Long? = null, // when the thingToDo or the project start
    val deadline: Long? = null, // to have a vision of the main targetGoal (exam's date, project's end, etc.)
    val description: String? = null,
    val type: String? = null,
    val importance: Int? = null, // thingToDo's impact on the smooth running of daily life
    val urgency: Int? = null,
    val isDraft: Boolean = false,
    val estimatedEmotions: Int = 1, // 0, 1 or 2 to express feelings on the thingToDo to accomplish
    val estimatedWorkingTime: Long? = null,
    val skillLevel: Int? = null, // thingToDo mastery level posses
    val creationDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "dependency_task_id")
    val dependencyId: Long? = null, // dependency on other tasks
    @ColumnInfo(name = "task_recurrence_id")
    val recurrenceInfosId: Long? = null, // recurrenceInfosId
    val categoryId: Long? = null,
    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: Long? = null,
    @ColumnInfo(name = "task_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
) : Parcelable {

    fun getCreationDateFormatted(): String {
        return DateFormat.getDateInstance().format(creationDate)
    }

    companion object {
        /**
         * Function that calculate urgency.
         * Delay between today date and deadline.
         * Otherwise, if no deadline, delay between today date and due date.
         * @return Int : between 2 and 10
         */
        fun calculusUrgency(todayDateMillis: Long, dueDate: Long, deadline: Long?): Int {
            val delay =
                if (deadline != null)
                    abs(deadline - todayDateMillis)
                else abs(dueDate - todayDateMillis)
            return when {
                delay <= 2 * DAY_IN_MILLIS -> 9
                delay <= 4 * DAY_IN_MILLIS -> 8
                delay <= 6 * DAY_IN_MILLIS -> 7
                delay <= 8 * DAY_IN_MILLIS -> 6
                delay <= 10 * DAY_IN_MILLIS -> 5
                delay <= 12 * DAY_IN_MILLIS -> 4
                delay <= 16 * DAY_IN_MILLIS -> 3
                delay <= 20 * DAY_IN_MILLIS -> 2
                else -> 1
            }
        }

        /**
         * To calculate priority :
         * (importance * urgency) / 10
         * @return int priority
         */
        fun calculatingPriority(
            priority: Int?,
            importance: Int? = null,
            urgency: Int? = null
        ): Int? {
            return if (importance != null && urgency != null)
                (importance * urgency) / 10
            else
                priority
        }

        fun getDateFormatted(date: Long?): String? {
            return if (date != null) {
                SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(date)
            } else null
        }

        fun getFormattedTime(time: Long?): String? {
            return if (time != null) {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                timeFormat.timeZone = TimeZone.getTimeZone("UTC")
                timeFormat.format(time)
            } else null
        }
    }
}

@Parcelize
data class TaskRecurrenceWithDays(
    @Embedded val taskRecurrence: TaskRecurrence, // recurrence details
    @Relation(
        parentColumn = "recurrence_id",
        entityColumn = "recurrenceId",
        associateBy = Junction(TaskRecurrenceDaysCrossRef::class)
    )
    val daysOfWeek: List<DaysOfWeek> // days associate
) : Parcelable {
    /**
     * Update recurring thingToDo depending with thingToDo's recurrence characteristics (delay, repetition frequency, etc.)
     * @param oldDueDate : old thingToDo due date
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
            // Set the new due date to the next occurrence of the thingToDo's due day
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


@Parcelize
data class ThingToDo(
    @Embedded
    val mainTask: Task,
    @Relation(
        parentColumn = "task_recurrence_id",
        entityColumn = "recurrence_id",
        entity = TaskRecurrence::class
    )
    val recurrence: TaskRecurrenceWithDays?,
    @Relation(parentColumn = "task_id", entityColumn = "parent_task_id", entity = Task::class)
    val subTasks: List<ThingToDo>,
    @Relation(parentColumn = "categoryId", entityColumn = "category_id", entity = Category::class)
    val category: Category?,
    @Relation(parentColumn = "task_id", entityColumn = "parent_id", entity = Reminder::class)
    val reminders: List<Reminder>,
    @Relation(parentColumn = "task_id", entityColumn = "dependency_task_id", entity = Task::class)
    val taskDependency: ThingToDo?,
    @Relation(parentColumn = "task_id", entityColumn = "parent_task_id", entity = Completion::class)
    val completions: List<Completion> // Historique d’achèvement

) : Parcelable {

    fun showCheckedState(): Boolean {
        return if (recurrence == null) false else completions.lastOrNull()?.isCompleted ?: false
    }

    fun isProject(): Boolean {
        return mainTask.type == Nature.PROJECT.name || subTasks.isNotEmpty()
    }

    fun countCompletedSubtasks(): Int {
        // Count direct sub tasks using last completion
        val directCompleted = subTasks.count { subtask ->
            subtask.completions.lastOrNull()?.isCompleted == true
        }
        // Add embedded completed sub tasks
        val nestedCompleted = subTasks.sumOf { it.countCompletedSubtasks() }
        return directCompleted + nestedCompleted
    }

    fun getNbSubTasks(): Int = subTasks.size

    fun getHabitSuccessRate(): Float? {
        val totalRepetitionCount = completions.size
        val successCount = completions.count { it.isCompleted }
        return if (totalRepetitionCount != 0)
            (successCount.toFloat() / totalRepetitionCount) * 100
        else null
    }

    fun isLate(todayDateMillis: Long, dueDate: Long): Boolean {
        return dueDate < todayDateMillis
    }
}

/**
 * Class with thingToDo completed info to analyse productivity (number of thingToDo achieved in a period of time).
 */
data class TtdAchieved(
    val completionDate: Long,
    val completedCount: Float
)

/**
 * Class with recurring thingToDo info for max and min streak.
 */
data class TtdStreakInfo(
    val title: String?,
    val streakInfo: Int?
)

/**
 * Describe type of objective targetGoal to help tracking progress.
 */
enum class AssessmentType { QUANTITY, DURATION, BOOLEAN }

/**
 * Evaluation class and entity of Room database.
 * Help to track and analyse details global objectives and their advancement.
 */
@Parcelize
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["task_id"],
            childColumns = ["parent_task_id"],
            onDelete = CASCADE
        ), ForeignKey(
            entity = Assessment::class,
            parentColumns = ["assessment_id"],
            childColumns = ["parent_assessment_id"],
            onDelete = CASCADE
        )]
)
data class Assessment(
    // parent id nullable
    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: Long? = null,
    @ColumnInfo(name = "parent_assessment_id")
    val parentAssessmentId: Long? = null,
    @ColumnInfo(name = "assessment_title")
    val title: String,
    val description: String? = null,
    val comment: String? = null,
    val targetGoal: Float,
    val unit: String,
    val type: String,
    @ColumnInfo(name = "assessment_due_date")
    val dueDate: Long,
    val isRecurrent: Boolean = false,
    val interval: RecurringTaskInterval? = null,
    val rehearsalEndDate: Long? = null,
    val score: Float? = null, // result that the user enter at the due date // maybe change name to "rating"
    val categoryId: Long? = null,
    @ColumnInfo(name = "assessment_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : Parcelable {

    fun getPercentageRating(): Float? {
        return if (targetGoal != 0F)
            (score ?: 0F) / targetGoal * 100
        else
            null
    }

    fun getFormattedDueDate(): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(dueDate)
    }
}

@Parcelize
@Entity
data class Category(
    @ColumnInfo(name = "category_title")
    val title: String,
    val description: String?,
    val color: Int? = null,
    @ColumnInfo(name = "parent_category_id")
    val parentCategoryId: Long? = null,
    @ColumnInfo(name = "category_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : Parcelable

data class TimeWorkedDistribution(val title: String?, val totalTimeWorked: Long?)

data class CategoryTasks(
    @Embedded
    val category: Category,
    @Relation(entity = Task::class, parentColumn = "category_id", entityColumn = "categoryId")
    val tasks: List<Task>
)

@Parcelize
@Entity(
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["task_id"],
        childColumns = ["parent_id"],
        onDelete = CASCADE
    )]
)
data class Reminder(
    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null, // attach to a thingToDo but also just parent reminder of the app
    val description: String? = null,
    val dueDate: Long,
    val isRecurrent: Boolean,
    val repetitionFrequency: RecurringTaskInterval? = null,
    @ColumnInfo(name = "reminder_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : Parcelable {

    fun isPassed(): Boolean {
        return dueDate < Calendar.getInstance().timeInMillis
    }

    fun getDueDateFormatted(): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(dueDate)
    }

    fun getTimeFormatted(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueDate)
    }
}

@Entity
data class WorkSession(
    val parentTaskId: Long,
    val duration: Long,
    val comment: String?,
    val date: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "work_session_id") val id: Long = 0
)

@Entity
data class Unit(
    val name: String,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "unit_id") val id: Long = 0
)

@Entity
data class PomodoroSession(
    val name: String,
    val workingDuration: Long = 30 * 60 * 1000,
    val restDuration: Long = 15 * 60 * 1000,
    val workSessionsCount: Int = 4,
    val specialMsg: String?,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "work_session_id") val id: Long = 0
)