package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlin.math.pow

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