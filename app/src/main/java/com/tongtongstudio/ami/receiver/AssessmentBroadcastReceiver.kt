package com.tongtongstudio.ami.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.ui.MainActivity

const val ASSESSMENT_CHANNEL_ID = "evaluation_channel"
const val ASSESSMENT_ID = "assessment_id"

/**
 * This class receive pending intent for mid term assessment.
 */
class AssessmentBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        //Log.e("Alarm on receive :","${intent?.getParcelableExtra<Assessment>(ASSESSMENT_ID)}")
        val assessment = intent?.getParcelableExtra<Assessment>(ASSESSMENT_ID)
        if (assessment != null) {
            showNotification(context, assessment)
        }
    }

    private fun showNotification(context: Context?, assessment: Assessment) {
        // open application with assessment
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(ASSESSMENT_ID, assessment)
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ASSESSMENT_CHANNEL_ID,
                context.getString(R.string.assessment_channel_description),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, ASSESSMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_small_notif)
            .setContentTitle(context.getString(R.string.assessment_notification_title))
            .setContentText(
                context.getString(
                    R.string.complete_assessment_notification_content,
                    assessment.title
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(assessment.id.toInt(), notification)
    }
}