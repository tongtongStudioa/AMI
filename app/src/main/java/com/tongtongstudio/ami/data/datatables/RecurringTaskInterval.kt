package com.tongtongstudio.ami.data.datatables

import android.content.res.Resources
import android.os.Parcelable
import androidx.room.TypeConverter
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.ui.dialog.Period
import kotlinx.parcelize.Parcelize
import java.util.Calendar
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

    /**
     * Update recurring task depending on task's characteristic (delay, repetition frequency, etc.)
     * @param ttd : task
     * @param checked : state
     * @return updated task
     */
    fun updateRecurringTask(ttd: Task, checked: Boolean): Task {
        val oldStartDate = ttd.dueDate!!
        val updatedStartDate = if (daysOfWeek != null) {
            findNextOccurrenceDayInWeek(oldStartDate, checked)
        } else {
            findNextOccurrenceDay(oldStartDate, checked)
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

    private fun findNextOccurrenceDayInWeek(oldDueDate: Long, checked: Boolean): RepeatProcess {
        var timesSkipped = 0
        val newStartDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val todayDateInMillis = timeInMillis
            timeInMillis = oldDueDate
            do {
                // manage skipped if it contains the day and it's not checked
                if (daysOfWeek!!.contains(get(Calendar.DAY_OF_WEEK)) && !checked)
                    timesSkipped++

                add(Calendar.DAY_OF_WEEK, 1)
                val nextDay = get(Calendar.DAY_OF_WEEK)

                // if we change of week so add intervalWeek if interval > 2 week (times = num of week interval)
                if (get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                    add(Calendar.DAY_OF_MONTH, (times - 1) * 7)
                }
                val nextDueDate = timeInMillis
                val notContainsAndBeforeToday =
                    !(daysOfWeek.contains(nextDay) && nextDueDate >= todayDateInMillis)
            } while (notContainsAndBeforeToday)
            // if list of recurrence days contains next deadline's day and it's after  today : set a new due date
            timeInMillis
        }
        return RepeatProcess(newStartDate, timesSkipped)
    }

    private fun findNextOccurrenceDay(oldDueDate: Long, checked: Boolean): RepeatProcess {
        var timesSkipped = 0
        var todayTimeInMillis: Long
        val newStartDate = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            todayTimeInMillis = timeInMillis
            // Set the new due date to the next occurrence of the task's due day
            timeInMillis = oldDueDate
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
                if (!checked)
                    timesSkipped++
            } while (timeInMillis < todayTimeInMillis)
            timeInMillis
        }
        return RepeatProcess(newStartDate, timesSkipped)
    }

    fun setStartDateSpecificDay(): Long {
        return if (daysOfWeek != null) {
            val startDate = Calendar.getInstance().run {
                while (get(Calendar.DAY_OF_WEEK) != daysOfWeek.first()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                timeInMillis
            }
            startDate
        } else Calendar.getInstance().timeInMillis
    }

    fun getNextOccurrenceDay(oldDueDate: Long, validate: Boolean): Long {
        val repeatProcess = if (daysOfWeek != null) {
            findNextOccurrenceDayInWeek(oldDueDate, validate)
        } else {
            findNextOccurrenceDay(oldDueDate, validate)
        }
        return repeatProcess.newDueDate
    }

    fun getRecurringIntervalReadable(resources: Resources): String {
        return if (times == 1 && daysOfWeek == null) {
            when (period) {
                Period.DAYS.name -> resources.getString(R.string.each_days)
                Period.WEEKS.name -> resources.getString(R.string.each_weeks)
                Period.MONTHS.name -> resources.getString(R.string.each_months)
                Period.YEARS.name -> resources.getString(R.string.each_years)
                else -> resources.getString(R.string.each_days)
            }
        } else if (daysOfWeek != null) {
            // TODO: create function to retrieve E from int : Mon, Tue, Wed, Thu, Fri (Lun, Mar, Mer, Jeu, Ven, ...)
            if (times == 1) resources.getString(
                R.string.weekly_interval,
                daysOfWeek.toString()
            ) else "On $daysOfWeek every $times weeks"
        } else {
            when (period) {
                Period.DAYS.name -> resources.getString(R.string.every_x_days, times)
                Period.WEEKS.name -> resources.getString(R.string.every_x_weeks, times)
                Period.MONTHS.name -> resources.getString(
                    R.string.every_x_months,
                    times
                )
                Period.YEARS.name -> resources.getString(R.string.every_x_years, times)
                else -> resources.getString(R.string.every_x_days, times)
            }
        }
    }
}

class RepeatProcess(val newDueDate: Long, val timesSkipped: Int = 0)