package com.tongtongstudio.ami.timer

import java.text.SimpleDateFormat
import java.util.*

object TrackingTimeUtility {

    fun getFormattedWorkingTime(
        timeInMillis: Long?,
        timerType: TimerType = TimerType.STOPWATCH
    ): String? {
        if (timeInMillis == null)
            return null
        return if (timerType == TimerType.COUNTDOWN) {
            SimpleDateFormat("mm:ss", Locale.getDefault()).format(timeInMillis)
        } else {
            val hours = timeInMillis / (3600 * 1000)
            val minutes = timeInMillis % (3600 * 1000) / (60 * 1000)
            val seconds = timeInMillis % (3600 * 1000) % (60 * 1000) / 1000
            when {
                hours > 0 -> String.format("%02dh %02dm %02ds", hours, minutes, seconds)
                minutes > 0 -> String.format("%02dm %02ds", minutes, seconds)
                else -> String.format("%02ds", seconds)
            }
        }
    }

    fun getFormattedEstimatedTime(estimatedTime: Long?): String? {
        if (estimatedTime == null)
            return null
        val hours = estimatedTime / (3600 * 1000)
        val minutes = estimatedTime % (3600 * 1000) / (60 * 1000)
        return when {
            hours > 0 -> String.format("%dh %dm", hours, minutes)
            else -> String.format("%dm", minutes)
        }
    }

    fun getFormattedTimeWorked(timeInMillis: Long?): String? {
        if (timeInMillis == null)
            return null
        val hours = timeInMillis / (3600 * 1000)
        val minutes = timeInMillis % (3600 * 1000) / (60 * 1000)
        val seconds = timeInMillis % (3600 * 1000) % (60 * 1000) / 1000
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }
}