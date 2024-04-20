package com.tongtongstudio.ami.timer

import android.os.Handler
import android.os.Looper
import com.tongtongstudio.ami.timer.TrackingConstants.LONG_REST_TIME_DURATION
import com.tongtongstudio.ami.timer.TrackingConstants.REST_TIME_DURATION
import com.tongtongstudio.ami.timer.TrackingConstants.TIMER_UPDATE_INTERVAL
import com.tongtongstudio.ami.timer.TrackingConstants.WORK_SESSIONS
import com.tongtongstudio.ami.timer.TrackingConstants.WORK_TIME_DURATION

class TimerStopWatch(
    private val listener: TimerListener
) {
    /**
     * Type to count work time : StopWatch or Timer
     */
    private var timerType: TimerType = TimerType.STOPWATCH

    /**
     * This variable is essential to count down timer (for pomodoro sessions)
     * and it's used for track duration even for StopWatch mode.
     * Equal workTime if type is CountDown and actualWorkTime else.
     */
    private var remainingTimeMillis: Long = 0L
    private var timeWorked: Long = 0L
    private val handler = Handler(Looper.myLooper()!!)
    private var isRunning = false
    private var isWorkSession = true
    private var sessionCount: Int = 0

    interface TimerListener {
        fun onTick(timeDisplayed: Long, timeWorked: Long)
        fun onWorkSessionFinish()
        fun onRestSessionFinish()
        fun onPomodoroSessionsFinish()
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                // add time worked
                if (isWorkSession)
                    timeWorked += TIMER_UPDATE_INTERVAL
                when (timerType) {
                    TimerType.COUNTDOWN -> {
                        countDownMechanism()
                    }
                    TimerType.STOPWATCH -> {
                        remainingTimeMillis += TIMER_UPDATE_INTERVAL
                        handler.postDelayed(this, TIMER_UPDATE_INTERVAL)
                    }
                }
                listener.onTick(remainingTimeMillis, timeWorked)
            }
        }
    }

    /**
     * Manage Pomodoro sessions, update work sessions.
     * When remaining time is lower than 0, update the count down timer to rest time (if work session is finish)
     * or work time (if rest is finish)
     */
    private fun countDownMechanism() {
        remainingTimeMillis -= TIMER_UPDATE_INTERVAL
        if (remainingTimeMillis >= 0) {
            handler.postDelayed(timerRunnable, TIMER_UPDATE_INTERVAL)
        } else {
            remainingTimeMillis = resetCountDown()
            handler.postDelayed(timerRunnable, TIMER_UPDATE_INTERVAL)
        }
    }

    /**
     * Update remaining time for pomodoro sessions depending on number of sessions passed.
     * @return new remaining time
     */
    private fun resetCountDown(): Long {
        return if (isWorkSession) {
            listener.onWorkSessionFinish()
            isWorkSession = false
            sessionCount++
            if (sessionCount == WORK_SESSIONS) // increase rest time if last rest session
                LONG_REST_TIME_DURATION
            else REST_TIME_DURATION
        } else {
            // reset to work session
            isWorkSession = true
            isRunning = sessionCount < WORK_SESSIONS
            if (isRunning) // alert user finish sessions or rest time
                listener.onRestSessionFinish()
            else listener.onPomodoroSessionsFinish()
            WORK_TIME_DURATION
        }
    }

    /**
     * Start the timer selected type
     * @return isRunning: boolean
     */
    fun start(): Boolean {
        if (!isRunning) {
            isRunning = true
            handler.postDelayed(timerRunnable, TIMER_UPDATE_INTERVAL)
        }
        return isRunning
    }

    fun pause(): Boolean {
        if (isRunning) {
            handler.removeCallbacks(timerRunnable)
            isRunning = false
            timeWorked =
                0L // reset timeWorked because of logic of sessions of time worked without interruption
            //listener.onTick(remainingTimeMillis,timeWorked)
        }
        return isRunning
    }

    fun reset(): Boolean {
        // stop
        isRunning = false
        isWorkSession = true
        handler.removeCallbacks(timerRunnable)

        // reset
        timeWorked = 0L //actualWorkTime --> choice of total reset or not
        //sessionCount = 0

        remainingTimeMillis = if (timerType == TimerType.COUNTDOWN) WORK_TIME_DURATION else 0L
        listener.onTick(remainingTimeMillis, timeWorked)

        return isRunning
    }

    fun updateType(type: TimerType) {
        timerType = type
        reset()
        sessionCount = 0
    }
}