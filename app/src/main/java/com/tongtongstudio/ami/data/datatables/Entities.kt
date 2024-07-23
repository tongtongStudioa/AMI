package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import android.text.format.DateUtils.DAY_IN_MILLIS
import androidx.room.*
import kotlinx.parcelize.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

const val PATTERN_FORMAT_DATE = "E dd/MM"

enum class Nature { PROJECT, TASK, EVENT }

@Parcelize
@Entity(tableName = "thing_to_do_table")
data class Ttd(
    val title: String,
    val priority: Int,
    @ColumnInfo(name = "task_due_date")
    val dueDate: Long, // when the task must be complete (to get ahead of the deadline)
    val startDate: Long? = null, // when the task or the project start
    val deadline: Long? = null, // to have a vision of the main goal (exam's date, project's end, etc.)
    val description: String? = null,
    val type: String? = null,
    val importance: Int? = null, // task's impact on the smooth running of daily life
    val urgency: Int? = null,

    val isCompleted: Boolean = false,
    val completionDate: Long? = null,
    val completedOnTime: Boolean? = null,
    // TODO: replace with estimated working time
    val estimatedTime: Long? = null,
    // TODO: replace with currentWorkingTime
    val actualWorkTime: Long? = null,
    val isRecurring: Boolean = false,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val repetitionFrequency: RecurringTaskInterval? = null,
    val totalRepetitionCount: Int = 0,
    val timesMissed: Int = 0,
    val successCount: Int = 0, // achievements number for recurrent tasks
    val dependency: Boolean? = null, // dependency on other people
    val skillLevel: Int? = null, // task mastery level posses
    val creationDate: Long = System.currentTimeMillis(),
    // TODO: add subjective score ? and commentary ?
    @ColumnInfo(name = "id_ttd")
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: Long? = null,
) : Parcelable {

    /**
     * This function update the current thing to do depend on recurring info and state state
     * @param state state
     * @return updated task
     */
    fun updateCheckedState(state: Boolean): Ttd {
        val updatedTask = when {
            // it is a recurring task
            isRecurring && repetitionFrequency != null -> repetitionFrequency.updateRecurringTask(
                this@Ttd,
                state
            )
            // it is checked
            state -> {
                val completedDateInMillis = Calendar.getInstance().timeInMillis
                val updatedState = this.copy(
                    isCompleted = true,
                    completionDate = completedDateInMillis
                )
                updatedState.copy(
                    completedOnTime = updatedState.hasBeenCompletedOnTime()
                )
            }
            // task is unchecked and it's not a recurring one
            else -> {
                this.copy(
                    isCompleted = false,
                    completionDate = null,
                    completedOnTime = null
                )
            }
        }
        return updatedTask
    }

    fun getHabitSuccessRate(): Float? {
        return if (totalRepetitionCount != 0)
            (successCount.toFloat() / totalRepetitionCount) * 100
        else null
    }

    /**
     * This function decide if a task is completed on time or not.
     * It compares completionDate and dueDate or completionDate and deadline if it was define
     * @return boolean
     */
    private fun hasBeenCompletedOnTime(): Boolean {
        return isCompleted && completionDate != null && (completionDate < dueDate || (deadline != null && completionDate < deadline))
    }

    fun getCreationDateFormatted(): String {
        return DateFormat.getDateInstance().format(creationDate)
    }

    fun isLate(): Boolean {
        val todayDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            timeInMillis
        }
        return dueDate < todayDate && isCompleted
    }

    companion object {
        /**
         * Function that calculate urgency.
         * Delay between due date and deadline otherwise, if no deadline, urgency = 9.
         * @return Int : between 2 and 10
         */
        fun calculusUrgency(todayDateMillis: Long, dueDate: Long, deadline: Long?): Int {
            val delay = if (deadline != null) abs(dueDate - deadline) else 9
            return when {
                delay <= 1 * DAY_IN_MILLIS -> 9
                delay <= 2 * DAY_IN_MILLIS -> 8
                delay <= 3 * DAY_IN_MILLIS -> 7
                delay <= 5 * DAY_IN_MILLIS -> 6
                delay <= 7 * DAY_IN_MILLIS -> 5
                delay <= 10 * DAY_IN_MILLIS -> 4
                delay <= 14 * DAY_IN_MILLIS -> 3
                delay <= 19 * DAY_IN_MILLIS -> 2
                else -> 1
            }
        }

        /**
         * To calculate priority :
         * 2 * (importance * urgency) / (importance + urgency)
         * @return int priority
         */
        fun calculatingPriority(priority: Int, importance: Int? = null, urgency: Int? = null): Int {
            // TODO: change way of calculus priority
            return if (importance != null && urgency != null)
                2 * (importance * urgency) / (importance + urgency)
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

// TODO: add category in data class
@Parcelize
data class TaskWithSubTasks(
    @Embedded
    val mainTask: Ttd,
    @Relation(parentColumn = "id_ttd", entityColumn = "parent_task_id", entity = Ttd::class)
    val subTasks: List<Ttd>
) : Parcelable {
    fun getNbSubTasksCompleted(): Int = subTasks.count { it.isCompleted }
    fun getNbSubTasks(): Int = subTasks.size
}

data class TtdAchieved(
    val completionDate: Long,
    val completedCount: Float
)

data class TtdStreakInfo(
    val title: String?,
    val streakInfo: Int?
)

/**
 * Evaluation class and entity of Room database.
 * Help to track and analyse details on objective advancement.
 */
@Parcelize
@Entity
data class Assessment(
    // TODO: change name parent id : assessment ref to an assessment = global objective
    // parent id nullable
    @ColumnInfo(name = "task_id")
    val taskId: Long,
    @ColumnInfo(name = "assessment_title")
    val title: String,
    val description: String? = null,
    // TODO: add comment on recurring task too
    val comment: String? = null,
    val goal: Int,
    // TODO: add type attribute : quantity, time or boolean
    val unit: String,
    @ColumnInfo(name = "assessment_due_date")
    val dueDate: Long,
    val isRecurrent: Boolean = false,
    val interval: RecurringTaskInterval? = null,
    val rehearsalEndDate: Long? = null,
    val score: Int? = null, // result that the user enter at the due date // maybe change name to "rating"
    @ColumnInfo(name = "assessment_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : Parcelable {

    fun getRating(): Float? {
        return if (goal != 0)
            (score ?: 0) / goal.toFloat()
        else
            null
    }

    fun getFormattedDueDate(): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(dueDate)
    }

    fun getSumUp(): String {
        val evaluationDateFormatted =
            SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(dueDate)
        // TODO: create a string resource
        return "$title on $evaluationDateFormatted"
    }
}

// TODO: add color
@Entity
data class Category(
    @ColumnInfo(name = "category_title")
    val title: String,
    val description: String?,
    @ColumnInfo(name = "parent_category_id")
    val parentCategoryId: Long? = null,
    @ColumnInfo(name = "category_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)

data class TimeWorkedDistribution(val title: String?, val totalTimeWorked: Long?)

data class CategoryTasks(
    @Embedded
    val category: Category,
    @Relation(entity = Ttd::class, parentColumn = "category_id", entityColumn = "categoryId")
    val tasks: List<Ttd>
)

// TODO: mark as passed if it is
@Entity
data class Reminder(
    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null, // attach to a task but also just parent reminder of the app
    val description: String? = null,
    val dueDate: Long,
    val isRecurrent: Boolean,
    val repetitionFrequency: RecurringTaskInterval? = null,
    @ColumnInfo(name = "reminder_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) {
    private fun getReminderDueDateFormatted(): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(dueDate)
    }

    private fun getReminderTimeFormatted(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueDate)
    }

    fun getReminderInformation(): String {
        val dueDateFormatted = getReminderDueDateFormatted()
        val dueTimeFormatted = getReminderTimeFormatted()
        // TODO: create string resource
        return "$dueDateFormatted at $dueTimeFormatted"
    }

    fun formatDueDate(): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dueDate
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(calendar.time)
    }
}

// TODO: Create TimeWorked sessions entry
// TODO: Create Unit entry
// TODO: Create Pomodoro sessions for later upgrade