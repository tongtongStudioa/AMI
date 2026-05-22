package com.tongtongstudio.ami.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.hilt.work.HiltWorker
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.ReminderNotification
import com.tongtongstudio.ami.receiver.REMINDER_CHANNEL_ID
import com.tongtongstudio.ami.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: Repository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i("SEND REMINDER NOTIF", "Get reminder id in worker")
        val reminderId = inputData.getLong("REMINDER_ID", -1)
        if (reminderId == -1L) {
            Log.i("SEND REMINDER NOTIF", "Failure : no id")
            return Result.failure()
        }
        Log.i("SEND REMINDER NOTIF", "Get reminder (id=$reminderId)")
        val reminderTask = repository.getReminderNotification(reminderId)
        Log.i("SEND REMINDER NOTIF", "Reminder id = ${reminderTask?.reminderId}")
        if (reminderTask == null) {
            Log.i("SEND REMINDER NOTIF","bug no return :( !!")
            return Result.failure()
        }
        Log.i("SEND REMINDER NOTIF", "Test if now time greater than due date")
        Log.i("SEND REMINDER NOTIF", "${System.currentTimeMillis() >= reminderTask.dueDate - 5 * 1000}")

        if (System.currentTimeMillis() >= reminderTask.dueDate - 5 * 1000) {
            sendNotification(applicationContext, reminderTask)
        }
        return Result.success()
    }

    private fun sendNotification(context: Context, reminder: ReminderNotification) {
        // TODO: Create notification helper
        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.viewPagerTrackingAndStatsFragment)
            .setArguments(bundleOf("task_id" to reminder.parentTaskId))
            .createPendingIntent()

        Log.i("SEND REMINDER NOTIF", "Parent task id : ${reminder.parentTaskId}")
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_small_notif)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentIntent(pendingIntent)
            .setContentText(context.getString(R.string.reminder_content_text, reminder.taskTitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        //Log.i("SEND REMINDER NOTIF", "Notify")
        notificationManager.notify(reminder.reminderId.toInt(), notification)
    }
}