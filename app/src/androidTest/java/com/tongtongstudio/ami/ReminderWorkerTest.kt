package com.tongtongstudio.ami

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.tongtongstudio.ami.notification.ReminderWorker
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ReminderWorkerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun reminderWorker_shouldRunAndSendNotification() {
        // GIVEN
        val reminderId = 1L

        val inputData = workDataOf(
            "REMINDER_ID" to reminderId
        )

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(inputData)
            .build()

        val workManager = WorkManager.getInstance(context)

        // WHEN
        workManager.enqueue(request).result.get()

        // Force execution
        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
        testDriver.setAllConstraintsMet(request.id)

        // THEN
        val workInfo = workManager.getWorkInfoById(request.id).get()

        assertEquals(workInfo?.state,WorkInfo.State.SUCCEEDED)
    }

    @Test
    fun reminderWorker_withDelay_shouldExecute() {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(request).result.get()

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!

        // Simule que le délai est passé
        testDriver.setInitialDelayMet(request.id)

        val workInfo = workManager.getWorkInfoById(request.id).get()

        assertEquals(workInfo?.state, WorkInfo.State.SUCCEEDED)
    }
}