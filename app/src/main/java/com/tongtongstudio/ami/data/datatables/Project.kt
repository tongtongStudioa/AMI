package com.tongtongstudio.ami.data.datatables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
@Entity(tableName = "project_table")
data class Project(
    val pjtName: String,
    val pjtPriority: Int,
    val pjtStartDate: Long?,
    val pjtDeadline: Long? = null,
    val pjtCategory: String? = null,
    val pjtDescription: String? = null,
    //@ColumnInfo(name = "protocol_repeatable") val protocolRepeatable: String? = null,
    @ColumnInfo(name = "pjt_estimated_time") val pjtEstimatedTime: Long? = null,
    @ColumnInfo(name = "pjt_work_time") val pjtWorkTime: Long? = null,
    @ColumnInfo(name = "is_pjt_completed") val isPjtCompleted: Boolean = false,
    val pjtCompletedDate: Long? = null,
    val pjtReminder: Long? = null,
    // unique properties
    val nb_sub_task: Int = 0,
    val nb_sub_tasks_completed: Int = 0,
    // unique thing to do's id
    @PrimaryKey(autoGenerate = true) val p_id: Long = 0,
    @ColumnInfo(name = "pjt_created_date") val pjtCreatedDate: Long = System.currentTimeMillis()
) : ThingToDo(pjtName, pjtPriority, pjtDeadline) {

    override fun getStartDate(): Long? {
        return pjtStartDate
    }

    override fun getStartDateFormatted(): String? {
        return if (pjtStartDate != null) {
            SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(deadline)
        } else null
    }

    override fun getCreatedDateFormatted(): String {
        return DateFormat.getDateInstance().format(pjtCreatedDate)
    }

    override fun getEstimatedTime(): Long? {
        return pjtEstimatedTime
    }

    override fun getWorkTime(): Long? {
        return pjtWorkTime
    }

    override fun isCompleted(): Boolean {
        return isPjtCompleted
    }

    override fun getReminderDate(): Long? {
        return pjtReminder
    }

    override fun getCompletedDate(): Long? {
        return pjtCompletedDate
    }

    override fun getCategory(): String? {
        return pjtCategory
    }

    override fun getDescription(): String? {
        return pjtDescription
    }
}