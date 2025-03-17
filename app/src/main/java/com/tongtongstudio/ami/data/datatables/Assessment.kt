package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Locale

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
            onDelete = ForeignKey.CASCADE
        ), ForeignKey(
            entity = Assessment::class,
            parentColumns = ["assessment_id"],
            childColumns = ["parent_assessment_id"],
            onDelete = ForeignKey.CASCADE
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