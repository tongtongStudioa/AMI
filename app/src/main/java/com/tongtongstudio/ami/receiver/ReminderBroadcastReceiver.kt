package com.tongtongstudio.ami.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.ui.MainActivity
import java.util.*

const val TASK_NAME_KEY = "TASK_NAME"
const val REMINDER_CUSTOM_INTERVAL = "reminder_custom_interval"
const val REMINDER_CHANNEL_ID: String = "task_channel_id"
const val REMINDER_DUE_DATE = "reminder_due_date"
const val REMINDER_ID = "reminder_id"

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val taskName = intent?.getStringExtra(TASK_NAME_KEY) ?: "thingToDo"
        val reminderDueDate =
            intent?.getLongExtra(REMINDER_DUE_DATE, Calendar.getInstance().timeInMillis)
                ?: Calendar.getInstance().timeInMillis
        val customReminderInterval = intent?.getParcelableArrayExtra(REMINDER_CUSTOM_INTERVAL)
            ?.get(0) as TaskRecurrenceWithDays?
        val reminderId = intent?.getLongExtra(REMINDER_ID, 0) ?: 0

        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // intent to open application
        val resultIntent = Intent(context, MainActivity::class.java)


        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                context.getString(R.string.reminder_channel_notifications_title),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_content_text, taskName))
            .setSmallIcon(R.drawable.ic_small_notif)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1, notification)
        scheduleNewReminder(context, taskName, reminderDueDate, customReminderInterval, reminderId)
    }

    private fun scheduleNewReminder(
        context: Context,
        taskName: String,
        reminderDueDate: Long,
        customReminderInterval: TaskRecurrenceWithDays?,
        reminderId: Long
    ) {
        if (customReminderInterval != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                putExtra(TASK_NAME_KEY, taskName)
                putExtra(REMINDER_DUE_DATE, reminderDueDate)
                putParcelableArrayListExtra(
                    REMINDER_CUSTOM_INTERVAL,
                    arrayListOf(customReminderInterval)
                )
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt(),
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val newReminderDueDate =
                customReminderInterval.getNextOccurrenceDay(reminderDueDate, true)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, newReminderDueDate, pendingIntent)
        }
    }
}
