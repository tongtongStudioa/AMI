package com.tongtongstudio.ami.data.datatables

import android.content.res.Resources
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.ui.dialog.recurring_task.Period
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

    fun getReadableTaskRecurrenceInfos(resources: Resources): String {
        return if (interval == 1) {
            when (frequency) {
                Period.DAYS.name -> resources.getString(R.string.each_days)
                Period.WEEKS.name -> resources.getString(R.string.each_weeks)
                Period.MONTHS.name -> resources.getString(R.string.each_months)
                Period.YEARS.name -> resources.getString(R.string.each_years)
                else -> resources.getString(R.string.each_days)
            }
        } else {
            when (frequency) {
                Period.DAYS.name -> resources.getString(R.string.every_x_days, interval)
                Period.WEEKS.name -> resources.getString(R.string.every_x_weeks, interval)
                Period.MONTHS.name -> resources.getString(
                    R.string.every_x_months,
                    interval
                )
                Period.YEARS.name -> resources.getString(R.string.every_x_years, interval)
                else -> resources.getString(R.string.every_x_days, interval)
            }
        }
    }
}