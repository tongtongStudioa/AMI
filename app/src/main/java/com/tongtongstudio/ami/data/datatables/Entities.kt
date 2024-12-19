package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import android.text.format.DateUtils.DAY_IN_MILLIS
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.parcelize.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

const val PATTERN_FORMAT_DATE = "E dd/MM"

enum class Nature { PROJECT, TASK }

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
        )]
)
data class Task(
    val title: String,
    val priority: Int?,
    @ColumnInfo(name = "task_due_date")
    val dueDate: Long?, // when the task must be complete (to get ahead of the deadline)
    val startDate: Long? = null, // when the task or the project start
    val deadline: Long? = null, // to have a vision of the main targetGoal (exam's date, project's end, etc.)
    val description: String? = null,
    val type: String? = null,
    val importance: Int? = null, // task's impact on the smooth running of daily life
    val urgency: Int? = null,
    val isDraft: Boolean = false,

    val isCompleted: Boolean = false,
    val completionDate: Long? = null,
    val completedOnTime: Boolean? = null,
    val estimatedWorkingTime: Long? = null,
    val currentWorkingTime: Long? = null,
    val isRecurring: Boolean = false,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val repetitionFrequency: RecurringTaskInterval? = null,
    val totalRepetitionCount: Int = 0,
    val timesMissed: Int = 0,
    val successCount: Int = 0, // achievements number for recurrent tasks
    val comment: String? = null,
    val dependency: Boolean? = null, // dependency on other people
    val skillLevel: Int? = null, // task mastery level posses
    val creationDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "task_id")
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
    fun updateCheckedState(state: Boolean = true, newCompletionDate: Long? = null): Task {
        val updatedTask = when {
            // it is a recurring task
            isRecurring && repetitionFrequency != null -> repetitionFrequency.updateRecurringTask(
                this@Task,
                state
            )
            // it is checked
            state -> {
                val completedDateInMillis = Calendar.getInstance().timeInMillis
                val updatedState = this.copy(
                    isCompleted = true,
                    completionDate = newCompletionDate ?: completedDateInMillis
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
        return isCompleted && completionDate != null && dueDate != null && (completionDate < dueDate || (deadline != null && completionDate < deadline))
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
        return dueDate != null && dueDate < todayDate && isCompleted
    }

    fun getCompletionDateFormatted(): String {
        return if (isCompleted && completionDate != null)
            DateFormat.getDateInstance().format(completionDate)
        else "null"
    }

    companion object {
        /**
         * Function that calculate urgency.
         * Delay between due date and deadline otherwise, if no deadline, urgency = 9.
         * @return Int : between 2 and 10
         */
        fun calculusUrgency(todayDateMillis: Long, dueDate: Long?, deadline: Long?): Int {
            val delay = if (deadline != null && dueDate != null) abs(dueDate - deadline) else 9
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
data class ThingToDo(
    @Embedded
    val mainTask: Task,
    @Relation(parentColumn = "task_id", entityColumn = "parent_task_id", entity = Task::class)
    val subTasks: List<Task>,
    @Relation(parentColumn = "categoryId", entityColumn = "category_id", entity = Category::class)
    val category: Category?,
    @Relation(parentColumn = "task_id", entityColumn = "parent_id", entity = Reminder::class)
    val reminders: List<Reminder>

) : Parcelable {
    fun isProject(): Boolean {
        return mainTask.type == Nature.PROJECT.name || subTasks.isNotEmpty()
    }

    fun getNbSubTasksCompleted(): Int = subTasks.count { it.isCompleted }
    fun getNbSubTasks(): Int = subTasks.size
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
    val parentId: Long? = null, // attach to a task but also just parent reminder of the app
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