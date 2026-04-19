package com.tongtongstudio.ami.domain.usecase

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.notification.AssessmentWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ScheduleAssessmentUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(assessments: List<Assessment>) {
        //Log.i("SCHEDULE ASSESSMENT", "Is assessments to schedule ? ${!assessments.isEmpty()}")
        assessments.forEach { assessment ->
            //Log.i("SCHEDULE ASSESSMENT", assessment.title + " to schedule ? ${assessment.dueDate > System.currentTimeMillis()}")
            if (assessment.dueDate > System.currentTimeMillis()) {
                scheduleAssessment(assessment)
            }
        }
    }

    private fun scheduleAssessment(assessment: Assessment) {
        val delay = assessment.dueDate - System.currentTimeMillis()
        if (delay <= 0) return

        val workRequest = OneTimeWorkRequestBuilder<AssessmentWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf("ASSESSMENT_ID" to assessment.id)
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "assessment_${assessment.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}