package com.tongtongstudio.ami.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tongtongstudio.ami.notification.TimerNotification
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_CANCEL
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_PAUSE
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_START

class TimerNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_CANCEL -> {
                // todo: stopAndReset chronometer
                TimerNotification.hideTimerNotification(context)
            }
            ACTION_PAUSE -> {
                // todo: pause chronometer
                TimerNotification.showTimerPaused(context)
            }
            ACTION_START -> {
                // todo: resume chronometer
                //TimerNotification.showTimerRunning(context, reminingtime)
            }
            /*ACTION_START -> {
                val minutesRemaining = PrefUtil.getTimerLength(context)
                val secondsRemaining = minutesRemaining * 60L
                val wakeUpTime = TimerActivity.setAlarm(context, TimerActivity.nowSeconds, secondsRemaining)
                PrefUtil.setTimerState(TimerActivity.TimerState.Running, context)
                PrefUtil.setSecondsRemaining(secondsRemaining, context)
                TimerNotification.showTimerRunning(context, wakeUpTime)
            }*/
        }
    }
}