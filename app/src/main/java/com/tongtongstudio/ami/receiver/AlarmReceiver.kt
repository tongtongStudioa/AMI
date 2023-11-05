package com.tongtongstudio.ami.receiver

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.ui.MainActivity

const val TASK_CHANNEL_ID: String = "task_channel_id"
const val TTD_NAME: String = "thing_to_do_name"
const val TTD_DESCRIPTION: String = "thing_to_do_description"

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // TODO: 09/07/2022 change this intent to go to timer of the task with link
        val intentDestination = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(
                    context,
                    0,
                    intentDestination,
                    PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(context, 0, intentDestination, 0)
            }
        val ttdName = intent.getStringExtra(TTD_NAME)
        val descriptionNotification = intent.getStringExtra(TTD_DESCRIPTION)

        val builder = NotificationCompat.Builder(context, TASK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground_simple)
            .setContentTitle(ttdName)
            .setContentText(descriptionNotification ?: "Description notification task")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            val notificationId = 1
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(notificationId, builder.build())
        }
    }
}