package com.tongtongstudio.ami.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.hilt.work.HiltWorker
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.receiver.ASSESSMENT_CHANNEL_ID
import com.tongtongstudio.ami.receiver.REMINDER_CHANNEL_ID
import com.tongtongstudio.ami.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AssessmentWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: Repository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        //Log.i("SCHEDULE ASSESSMENT WORKER", "In assessment worker")
        val assessmentId = inputData.getLong("ASSESSMENT_ID", -1)
        //Log.i("SCHEDULE ASSESSMENT WORKER", assessmentId.toString())
        if (assessmentId == -1L) {
            return Result.failure()
        }
        val assessment = repository.getAssessment(assessmentId)


        if (System.currentTimeMillis() >= assessment.dueDate) {
            //Log.i("SCHEDULE ASSESSMENT WORKER", "Send notif")
            sendNotification(applicationContext, assessment)
        }
        return Result.success()
    }

    private fun sendNotification(context: Context, assessment: Assessment) {
        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.completeAssessmentDialogFragment)
            .setArguments(bundleOf("assessment" to assessment))
            .createPendingIntent()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ASSESSMENT_CHANNEL_ID,
                "Assessments",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_small_notif)
            .setContentTitle(context.getString(R.string.assessment_notification_title))
            .setContentText(context.getString(R.string.complete_assessment_notification_content, assessment.title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        //Log.i("SEND ASSESSMENT NOTIF", "Notify")
        notificationManager.notify(assessment.id.toInt(), notification)
    }
}