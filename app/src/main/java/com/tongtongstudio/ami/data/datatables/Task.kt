package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
@Entity(tableName = "task_table")
data class Task(
    val taskName: String,
    val taskPriority: Int,
    val taskStartDate: Long?,
    val taskDeadline: Long? = null,
    val taskCategory: String? = null,
    val taskDescription: String? = null,
    @ColumnInfo(name = "estimated_time") val taskEstimatedTime: Long? = null,
    @ColumnInfo(name = "work_time") val taskWorkTime: Long? = null, // in millis
    val isTaskCompleted: Boolean = false,
    val taskCompletedDate: Long? = null, // date time in millis, last date completed date for recurring task
    val taskReminder: Long? = null, //reminder date in millis

    // *** unique properties *** //
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
    @ColumnInfo(name = "recurring_task_interval") val recurringTaskInterval: RecurringTaskInterval? = null,
    val streak: Int = 0, ///streak count if it's a recurring task
    val maxStreak: Int = 0,
    val nbCompleted: Int = 0,
    val nbRecurrence: Int = 0,
    // to track progress and regression
    @ColumnInfo(name = "task_evaluation_description") val taskEvaluationDescription: String? = null,
    // todo: make this type change with the need of evaluation : boolean (ex: 50min of meditation by day on saturday 6 ? true or false)
    @ColumnInfo(name = "task_evaluation_goal") val taskEvaluationGoal: Double? = null, // quantity to measure
    @ColumnInfo(name = "task_evaluation_unit") val taskEvaluationUnit: String? = null,
    @ColumnInfo(name = "task_evaluation_date") val taskEvaluationDate: Long? = null,
    @ColumnInfo(name = "task_evaluation_rating") val taskEvaluationRating: Double? = null,
    // to calculate more objective priority level
    @ColumnInfo(name = "is_someone_dependent") val dependency: Boolean? = null, // 1 or 0
    @ColumnInfo(name = "competence_mastering") val userLevel: Int? = null, // 1 to 5
    val probability: Int? = null, // 1 to 5
    val severity: Int? = null, // 1 to 5
    val criticality: Int? = if (probability != null && severity != null) probability * severity else null,
    @ColumnInfo(name = "project_id") val projectId: Long? = null,
    // unique thing to do's id
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_date") val taskCreatedDate: Long = System.currentTimeMillis(),
) : ThingToDo(taskName, taskPriority, taskDeadline), Parcelable {

    override fun getStartDate(): Long? {
        return taskStartDate
    }

    override fun getStartDateFormatted(): String? {
        return if (taskStartDate != null) {
            SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(taskStartDate)
        } else null
    }

    override fun getCreatedDateFormatted(): String {
        return DateFormat.getDateInstance().format(taskCreatedDate)
    }

    override fun getEstimatedTime(): Long? {
        return taskEstimatedTime
    }

    override fun getWorkTime(): Long? {
        return taskWorkTime
    }

    override fun isCompleted(): Boolean {
        return isTaskCompleted
    }

    override fun getReminderDate(): Long? {
        return taskReminder
    }

    override fun getCompletedDate(): Long? {
        return taskCompletedDate
    }

    override fun getCategory(): String? {
        return taskCategory
    }

    override fun getDescription(): String? {
        return taskDescription
    }

    override fun getPriorityUpgraded(): Int? {
        return if (userLevel != null && dependency != null) {
            (priority + userLevel) - dependency.toString().toInt()
        } else
            null
    }
}