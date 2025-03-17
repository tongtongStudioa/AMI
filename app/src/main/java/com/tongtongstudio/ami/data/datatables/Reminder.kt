package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Parcelize
@Entity(
    foreignKeys = [ForeignKey(
        entity = Task::class,
        parentColumns = ["task_id"],
        childColumns = ["parent_id"],
        onDelete = ForeignKey.CASCADE
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