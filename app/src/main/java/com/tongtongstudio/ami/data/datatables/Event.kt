package com.tongtongstudio.ami.data.datatables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
@Entity(tableName = "event_table")
data class Event(
    // shared properties by event, project and event class
    @ColumnInfo(name = "event_name") val eventName: String,
    @ColumnInfo(name = "event_priority") val eventPriority: Int,
    @ColumnInfo(name = "event_start_date") val eventStartDate: Long?,
    @ColumnInfo(name = "event_deadline") val eventDeadline: Long? = null,
    val eventDescription: String? = null,
    //@ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
    //@ColumnInfo(name = "protocol_repeatable") val protocolRepeatable: String? = null,
    // TODO: change these lines to avoid to much codes
    @ColumnInfo(name = "estimated_time") val eventEstimatedTime: Long? = null,
    @ColumnInfo(name = "work_time") val eventWorkTime: Long? = null,
    @ColumnInfo(name = "is_event_completed") val isEventCompleted: Boolean = false,
    //val completedDate: String? = null,
    @ColumnInfo(name = "event_reminder") val eventReminder: Long? = null,
    // unique properties
    @ColumnInfo(name = "is_spread") val isSpread: Boolean = false,
    // unique thing to do's id
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_date") val eventCreatedDate: Long = System.currentTimeMillis(),

    // TODO: create a completedDate when event is passed
    ) : ThingToDo(eventName, eventPriority, eventDeadline) {
    override fun getCreatedDateFormatted(): String {
        return DateFormat.getDateInstance().format(eventCreatedDate)
    }

    override fun getStartDateFormatted(): String? {
        return if (eventStartDate != null) {
            SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(deadline)
        } else null
    }

    override fun getStartDate(): Long? {
        return eventStartDate
    }

    override fun getDescription(): String? {
        return eventDescription
    }
}