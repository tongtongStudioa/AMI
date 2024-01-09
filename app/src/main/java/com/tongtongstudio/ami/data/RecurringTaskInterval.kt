package com.tongtongstudio.ami.data

import android.os.Parcelable
import androidx.room.TypeConverter
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.ui.dialog.Period
import kotlinx.parcelize.Parcelize
import java.util.*
import kotlin.math.pow


class Converters {
    private fun retrieveRecurringInfo(codeRef: String): RecurringTaskInterval {
        val codeList = codeRef.split("/")
        val times = codeList[0].toInt()
        val period = codeList[1]
        val daysOfWeek: List<Int>? = if (codeList.size > 2) {
            val daysOfWeekString = codeList[2].split(";")
            val daysOfWeekInt = ArrayList<Int>()
            for (string in daysOfWeekString) {
                daysOfWeekInt.add(string.toInt())
            }
            daysOfWeekInt
        } else null
        return RecurringTaskInterval(times, period, daysOfWeek)
    }

    @TypeConverter
    fun stringToRecurringTaskInterval(codeRef: String?): RecurringTaskInterval? {
        return codeRef?.let { retrieveRecurringInfo(it) }
    }

    @TypeConverter
    fun objectToString(interval: RecurringTaskInterval?): String? {
        return if (interval != null) {
            val daysOfWeek: String =
                if (interval.daysOfWeek != null) {
                    var daysOfWeekString = "/"
                    for (string in interval.daysOfWeek) {
                        daysOfWeekString += string
                        if (string != interval.daysOfWeek.last())
                            daysOfWeekString += ';'
                    }
                    daysOfWeekString
                } else ""

            interval.times.toString() + "/" + interval.period + daysOfWeek
        } else null
    }
}

@Parcelize
class RecurringTaskInterval(
    val times: Int, // every 1, 2, 3 or 18 ...
    val period: String, // ... (hours?), days, week, month, year.
    val daysOfWeek: List<Int>? = null // on Monday and Wednesday for example
) : Parcelable {

    /**
     * Create a new interval with user's feedback to increase or decrease last recurring interval.
     * @param userFeedback
     * @return RecurringTaskInterval
     */
    fun createNewInterval(userFeedback: Boolean): RecurringTaskInterval {
        return if (userFeedback) increaseInterval() else decreaseInterval()
    }

    private fun increaseInterval(): RecurringTaskInterval {
        // TODO: find a correct way to increase interval
        val newTimes = times.toDouble().pow(2).toInt()
        return RecurringTaskInterval(newTimes, period, daysOfWeek)
    }

    private fun decreaseInterval(): RecurringTaskInterval {
        val newTimes = times - 1
        return RecurringTaskInterval(newTimes, period, daysOfWeek)
    }

    fun updateRecurringTask(ttd: Ttd, checked: Boolean): Ttd {
        val oldStartDate = ttd.dueDate
        val updatedStartDate = if (daysOfWeek != null) {
            findNextOccurrenceDay(oldStartDate, checked)
        } else {
            findNextStartDate(oldStartDate, checked)
        }
        val newDueDateDate = updatedStartDate.newDueDate
        val timesSkipped = updatedStartDate.timesSkipped

        // TODO: 25/10/2022 how dismiss a miss check ? how count when task were completed or not ?

        // all attributes to update
        val isTaskEnding = if (ttd.deadline != null) newDueDateDate > ttd.deadline else false
        val newCurrentStreak = if (timesSkipped == 0) { // if no skipped task
            ttd.currentStreak + 1 // add one day of streak
        } else 0 // else reset current streak
        val newSuccessCount = ttd.successCount + if (checked) 1 else 0
        val newMaxStreak = if (newCurrentStreak > ttd.maxStreak) newCurrentStreak else ttd.maxStreak
        val newCountRepetition =
            ttd.totalRepetitionCount + if (timesSkipped > 0) timesSkipped else 1

        val updatedTask = ttd.copy(
            isCompleted = isTaskEnding,
            completionDate = if (isTaskEnding) ttd.dueDate else null,
            dueDate = if (isTaskEnding) ttd.dueDate else newDueDateDate,
            currentStreak = newCurrentStreak,
            successCount = newSuccessCount,
            timesMissed = ttd.timesMissed + timesSkipped,
            maxStreak = newMaxStreak,
            totalRepetitionCount = newCountRepetition
        )
        return updatedTask
    }

    private fun findNextOccurrenceDay(oldStartDate: Long, checked: Boolean): UpdatedStartDate {
        var timesSkipped = 0
        val newStartDate = Calendar.getInstance().run {
            val todayTimeInMillis = timeInMillis
            timeInMillis = oldStartDate
            do {
                if (daysOfWeek!!.contains(get(Calendar.DAY_OF_WEEK)) && checked)
                    timesSkipped++
                add(Calendar.DAY_OF_WEEK, 1)
                val nextDay = get(Calendar.DAY_OF_WEEK)
                if (get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) { // if we change of week so add intervalWeek if interval > 2 week
                    add(Calendar.DAY_OF_MONTH, (times - 1) * 7)
                }
            } while (!daysOfWeek!!.contains(nextDay) && todayTimeInMillis > timeInMillis) // if list of recurrence days contains next deadline's day so set this deadline
            timeInMillis
        }
        return UpdatedStartDate(newStartDate, timesSkipped)
    }

    private fun findNextStartDate(oldStartDate: Long, checked: Boolean): UpdatedStartDate {
        var timesSkipped = 0
        var todayTimeInMillis: Long
        val newStartDate = Calendar.getInstance().run {
            todayTimeInMillis = timeInMillis
            // Set the new due date to the next occurrence of the task's due day
            timeInMillis = oldStartDate
            do {
                when (period) {
                    Period.DAYS.name -> add(
                        Calendar.DAY_OF_MONTH,
                        times * 1
                    )
                    Period.WEEKS.name -> add(
                        Calendar.DAY_OF_MONTH,
                        times * 7
                    )
                    Period.MONTHS.name -> add(
                        Calendar.MONTH,
                        times * 1
                    )
                    Period.YEARS.name -> add(
                        Calendar.YEAR,
                        times * 1
                    )
                    else -> add(Calendar.DAY_OF_MONTH, 0)
                }
                if (checked)
                    timesSkipped++
            } while (timeInMillis < todayTimeInMillis)
            timeInMillis
        }
        return UpdatedStartDate(newStartDate, timesSkipped)
    }

    // TODO: problem get string method without context
    /*fun getStringReadable(): String {
        return if (times == 1 && daysOfWeek == null) {
            when (period) {
                Period.DAYS.name -> getString(R.string.each_days)
                Period.WEEKS.name -> getString(R.string.each_weeks)
                Period.MONTHS.name -> getString(R.string.each_months)
                Period.YEARS.name -> getString(R.string.each_years)
                else -> getString(R.string.each_days)
            }
        } else if (daysOfWeek != null) {
            if (times == 1) {
                "Weekly on $daysOfWeek"

            } else "On $daysOfWeek every $times weeks"
        } else {
            when (period) {
                Period.DAYS.name -> getString(R.string.every_x_days, times)
                Period.WEEKS.name -> getString(R.string.every_x_weeks, times)
                Period.MONTHS.name -> getString(
                    R.string.every_x_months,
                    times
                )
                Period.YEARS.name -> getString(R.string.every_x_years, times)
                else -> getString(R.string.every_x_days, times)
            }
        }
    }*/
}

class UpdatedStartDate(val newDueDate: Long, val timesSkipped: Int = 0)