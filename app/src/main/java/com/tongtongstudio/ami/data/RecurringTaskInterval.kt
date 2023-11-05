package com.tongtongstudio.ami.data

import android.os.Parcelable
import androidx.room.TypeConverter
import kotlinx.parcelize.Parcelize


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
) : Parcelable
