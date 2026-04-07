package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import android.text.format.DateUtils.DAY_IN_MILLIS
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.SET_NULL
import androidx.room.Index
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
            childColumns = ["category_id"],
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
        )],
    indices = [
        Index("parent_task_id"),
        Index("task_recurrence_id")
    ]
)
data class Task(
    val title: String,
    val priority: Int?,
    @ColumnInfo(name = "task_due_date")
    val dueDate: Long?, // when the task must be complete (to get ahead of the deadline)
    @ColumnInfo(defaultValue = "NULL")
    val startDate: Long? = null, // when the task or the project start
    @ColumnInfo(defaultValue = "NULL")
    val deadline: Long? = null, // to have a vision of the main targetGoal (exam's date, project's end, etc.)
    @ColumnInfo(defaultValue = "NULL")
    val description: String? = null,
    @ColumnInfo(defaultValue = "TASK")
    val nature: String = Nature.TASK.name,
    @ColumnInfo(defaultValue = "NOT_STARTED")
    val status: String = Status.NOT_STARTED.name,
    @ColumnInfo(defaultValue = "NULL")
    val importance: Int? = null, // task's impact on the smooth running of daily life
    @ColumnInfo(defaultValue = "NULL")
    val urgency: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val isDraft: Boolean = false,

    @ColumnInfo(defaultValue = "1")
    val estimatedEmotions: Int = 1, // 0, 1 or 2 to express feelings on the thingToDo to accomplish
    @ColumnInfo(defaultValue = "NULL")
    val estimatedWorkingTime: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val skillLevel: Int? = null, // thingToDo mastery level posses
    val creationDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "dependency_task_id", defaultValue = "NULL")
    val dependencyId: Long? = null, // dependencyId on other tasks
    @ColumnInfo(name = "task_recurrence_id", defaultValue = "NULL")
    val recurrenceInfosId: Long? = null, // recurrenceInfosId
    @ColumnInfo(name = "category_id", defaultValue = "NULL")
    val categoryId: Long? = null,
    @ColumnInfo(name = "parent_task_id", defaultValue = "NULL")
    val parentTaskId: Long? = null,
    @ColumnInfo(name = "task_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0
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
        fun calculusUrgency(todayDateMillis: Long, dueDate: Long?, deadline: Long?): Int {
            val delay =
                if (deadline != null)
                    abs(deadline - todayDateMillis)
                else if (dueDate != null)
                    abs(dueDate - todayDateMillis)
                else 0
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
        ): Int {
            return when {
                importance != null && urgency != null ->
                    (importance + urgency) / 2
                priority != null -> priority
                importance != null -> importance
                urgency != null -> urgency
                else -> 5
            }
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