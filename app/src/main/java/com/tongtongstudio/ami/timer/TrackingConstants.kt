package com.tongtongstudio.ami.timer

object TrackingConstants {
    const val ACTION_TIMER_TYPE_COUNTDOWN: String = "action_change_timer_type_to_countdown"
    const val ACTION_TIMER_TYPE_STOPWATCH: String = "action_change_timer_type_to_stopwatch"
    const val ACTION_SHOW_TRACKING_FRAGMENT: String = "action_show_tracking_fragment"
    const val ACTION_ON_WORKING_SESSION_FINISH = "on_working_session_finish"
    const val ACTION_ON_REST_SESSION_FINISH = "on_rest_session_finish"

    /**
     *  1s
     */
    const val TIMER_UPDATE_INTERVAL = 1000L

    // TODO: make this updatable by user in preferences
    /**
     * Duration of work time sessions for pomodoro.
     * 25 min in millis
     */
    const val WORK_TIME_DURATION: Long = 25 * 60 * 1000L

    /**
     * Duration of rest time between pomodoro sessions.
     * 5 min in millis
     */
    const val REST_TIME_DURATION: Long = 5 * 60 * 1000L

    /**
     * Duration of long rest time after 4 pomodoro sessions.
     * 15 min in millis
     */
    const val LONG_REST_TIME_DURATION: Long = 5 * 3 * 60 * 1000L

    /**
     * Count of number sessions for pomodoro.
     */
    const val WORK_SESSIONS: Int = 4
}