package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

const val PATTERN_FORMAT_DATE = "E dd/MM"
enum class Nature { PROJECT, TASK, EVENT }

@Parcelize
open class ThingToDo(
    @Ignore open val name: String,
    @Ignore val priority: Int,
    @Ignore val deadline: Long?
) : Parcelable {

    open fun getCreatedDateFormatted(): String {
        return "no_date"
    }

    open fun getStartDateFormatted(): String? {
        return null
    }

    open fun getDeadlineFormatted(): String? {
        return if (deadline != null) {
            SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(deadline)
        } else null
    }

    open fun getStartDate(): Long? {
        return null
    }

    open fun getEstimatedTime(): Long? {
        return 0
    }

    open fun getWorkTime(): Long? {
        return 0
    }

    open fun isCompleted(): Boolean {
        return false
    }

    open fun getReminderDate(): Long? {
        return 0
    }

    open fun getCompletedDate(): Long? {
        return 0L
    }

    open fun getCategory(): String? {
        return null
    }

    open fun getDescription(): String? {
        return null
    }

    open fun getPriorityUpgraded(): Int? {
        return null
    }
}