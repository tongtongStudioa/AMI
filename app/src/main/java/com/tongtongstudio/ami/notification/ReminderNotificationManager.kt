package com.tongtongstudio.ami.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.AlarmManagerCompat
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.RecurringTaskInterval
import com.tongtongstudio.ami.receiver.AlarmReceiver
import com.tongtongstudio.ami.receiver.TASK_CHANNEL_ID
import com.tongtongstudio.ami.receiver.TTD_DESCRIPTION
import com.tongtongstudio.ami.receiver.TTD_NAME
import com.tongtongstudio.ami.ui.dialog.Period

class ReminderNotificationManager(private val context: Context) {
    /*init {
        createNotificationChannel()
    }*/

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Register the channel with the system
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun scheduleReminder(
        timeReminder: Long?,
        taskTitle: String,
        taskDescription: String,
        recurringTaskInterval: RecurringTaskInterval?
    ) {
        timeReminder?.let {
            val notifyIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(TTD_NAME, taskTitle)
                putExtra(TTD_DESCRIPTION, taskDescription)
            }

            val notifyPendingIntent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getBroadcast(
                        context, 0, notifyIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    PendingIntent.getBroadcast(context, 0, notifyIntent, Intent.FILL_IN_ACTION)
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPermission: Boolean = alarmManager.canScheduleExactAlarms()
                if (!hasPermission) {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    }
                    context.startActivity(intent)
                }
            }

            if (recurringTaskInterval != null) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    timeReminder,
                    getIntervalInMillis(recurringTaskInterval),
                    notifyPendingIntent
                )
            } else {
                AlarmManagerCompat.setExact(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    timeReminder,
                    notifyPendingIntent
                )
            }
        }
    }

    private fun getIntervalInMillis(recurringTaskInterval: RecurringTaskInterval): Long {
        return when (recurringTaskInterval.period) {
            Period.DAYS.name -> recurringTaskInterval.times * AlarmManager.INTERVAL_DAY
            Period.WEEKS.name -> recurringTaskInterval.times * AlarmManager.INTERVAL_DAY * 7
            Period.MONTHS.name -> recurringTaskInterval.times * AlarmManager.INTERVAL_DAY * 30
            Period.YEARS.name -> recurringTaskInterval.times * AlarmManager.INTERVAL_DAY * 365
            else -> recurringTaskInterval.times * AlarmManager.INTERVAL_DAY
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.title_notification_channel)
            val descriptionText = context.getString(R.string.description_notification_channel)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(TASK_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}