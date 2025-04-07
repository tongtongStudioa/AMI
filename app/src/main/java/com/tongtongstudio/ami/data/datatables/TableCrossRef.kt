package com.tongtongstudio.ami.data.datatables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE

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
            parentColumns = ["day_id"],
            childColumns = ["dayId"],
            onDelete = CASCADE
        )
    ]
)
data class TaskRecurrenceDaysCrossRef(
    @ColumnInfo(name = "recurrenceId") val recurrenceId: Long,
    @ColumnInfo(name = "dayId") val dayId: Int
)