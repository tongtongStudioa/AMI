package com.tongtongstudio.ami.timer

import java.text.SimpleDateFormat
import java.util.*

object TrackingTimeUtility {

    fun getFormattedWorkTime(timeInMillis: Long, timerType: TimerType): String {
        val timeFormat = if (timerType == TimerType.STOPWATCH) {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        } else SimpleDateFormat("mm:ss", Locale.getDefault())
        timeFormat.timeZone = TimeZone.getTimeZone("UTC")
        return timeFormat.format(timeInMillis)
    }
}