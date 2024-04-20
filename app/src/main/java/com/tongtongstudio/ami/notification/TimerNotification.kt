package com.tongtongstudio.ami.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.receiver.TimerNotificationActionReceiver
import com.tongtongstudio.ami.ui.MainActivity
import java.text.SimpleDateFormat

class TimerNotification {

    companion object {
        const val CHANNEL_ID_TIMER = "timer_notifications"
        const val CHANNEL_NAME_TIMER = "Tracking time working notifications"
        const val NOTIFICATION_TIMER_ID = 1
        const val ACTION_START = "action_start_timer"
        const val ACTION_PAUSE = "action_pause_timer"
        const val ACTION_CANCEL = "action_cancel_tracking_service"

        fun showTimerExpired(context: Context) {
            val startIntent = Intent(context, TimerNotificationActionReceiver::class.java)
            startIntent.action = ACTION_START
            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val nBuilder = getBasicNotificationBuilder(context, CHANNEL_ID_TIMER)
            nBuilder.setContentTitle("Timer Expired!")
                .setContentText("Start again?")
                .setContentIntent(getPendingIntentWithStack(context, MainActivity::class.java))
                .addAction(R.drawable.ic_play_arrow, "Start", startPendingIntent)

            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.createNotificationChannel(CHANNEL_ID_TIMER, CHANNEL_NAME_TIMER)

            nManager.notify(NOTIFICATION_TIMER_ID, nBuilder.build())
        }

        fun showTimerRunning(context: Context, wakeUpTime: Long) {
            val stopIntent = Intent(context, TimerNotificationActionReceiver::class.java)
            stopIntent.action = ACTION_CANCEL
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val pauseIntent = Intent(context, TimerNotificationActionReceiver::class.java)
            pauseIntent.action = ACTION_PAUSE
            val pausePendingIntent = PendingIntent.getBroadcast(
                context,
                0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)

            val contentText = "Work time : ${df.format(wakeUpTime)}"
            val nBuilder = getBasicNotificationBuilder(context, CHANNEL_ID_TIMER)
            nBuilder.setContentTitle("Tracking work time")
                .setContentText(contentText)
                .setContentIntent(getPendingIntentWithStack(context, MainActivity::class.java))
                .setOngoing(true)
                .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
                .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)

            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.createNotificationChannel(CHANNEL_ID_TIMER, CHANNEL_NAME_TIMER)

            nManager.notify(NOTIFICATION_TIMER_ID, nBuilder.build())
        }

        fun showTimerPaused(context: Context) {
            val resumeIntent = Intent(context, TimerNotificationActionReceiver::class.java)
            resumeIntent.action = ACTION_START
            val resumePendingIntent = PendingIntent.getBroadcast(
                context,
                0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val nBuilder = getBasicNotificationBuilder(context, CHANNEL_ID_TIMER)
            nBuilder.setContentTitle("Timer is paused.")
                .setContentText("Resume?")
                .setContentIntent(getPendingIntentWithStack(context, MainActivity::class.java))
                .setOngoing(true)
                .addAction(R.drawable.ic_play_arrow, "Resume", resumePendingIntent)

            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.createNotificationChannel(CHANNEL_ID_TIMER, CHANNEL_NAME_TIMER)

            nManager.notify(NOTIFICATION_TIMER_ID, nBuilder.build())
        }

        fun hideTimerNotification(context: Context) {
            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.cancel(NOTIFICATION_TIMER_ID)
        }

        private fun getBasicNotificationBuilder(context: Context, channelId: String)
                : NotificationCompat.Builder {
            return NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground_simple)
                .setAutoCancel(true)
                .setDefaults(0)
        }

        private fun <T> getPendingIntentWithStack(
            context: Context,
            javaClass: Class<T>
        ): PendingIntent {
            val resultIntent = Intent(context, javaClass)
            resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addParentStack(javaClass)
            stackBuilder.addNextIntent(resultIntent)

            return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun NotificationManager.createNotificationChannel(
            channelID: String,
            channelName: String
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelImportance = NotificationManager.IMPORTANCE_DEFAULT
                val nChannel = NotificationChannel(channelID, channelName, channelImportance)
                /*nChannel.enableLights(true)
                nChannel.lightColor = Color.BLUE*/
                this.createNotificationChannel(nChannel)
            }
        }
    }

}