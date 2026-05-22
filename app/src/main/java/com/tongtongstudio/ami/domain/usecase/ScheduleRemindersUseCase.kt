package com.tongtongstudio.ami.domain.usecase

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.notification.ReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ScheduleRemindersUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            //if (reminder.dueDate > System.currentTimeMillis())
            scheduleReminder(reminder)
        }
    }

    private fun scheduleReminder(reminder: Reminder) {
        val delay = reminder.dueDate - System.currentTimeMillis()
        if (delay < 0) return

        Log.i("SEND REMINDER NOTIF", "Create work request")
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf("REMINDER_ID" to reminder.id)
            )
            .build()
        //Log.i("SEND REMINDER NOTIF", "Work request created")
        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_${reminder.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}