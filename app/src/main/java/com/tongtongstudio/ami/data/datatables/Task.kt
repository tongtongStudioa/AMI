package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import android.text.format.DateUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

@Parcelize
@Entity(
    tableName = "task_table", foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["category_id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ), ForeignKey(
            entity = Task::class,
            parentColumns = ["task_id"],
            childColumns = ["parent_task_id"],
            onDelete = ForeignKey.CASCADE
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
                delay <= 1 * DateUtils.DAY_IN_MILLIS -> 9
                delay <= 2 * DateUtils.DAY_IN_MILLIS -> 8
                delay <= 3 * DateUtils.DAY_IN_MILLIS -> 7
                delay <= 5 * DateUtils.DAY_IN_MILLIS -> 6
                delay <= 7 * DateUtils.DAY_IN_MILLIS -> 5
                delay <= 10 * DateUtils.DAY_IN_MILLIS -> 4
                delay <= 14 * DateUtils.DAY_IN_MILLIS -> 3
                delay <= 19 * DateUtils.DAY_IN_MILLIS -> 2
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