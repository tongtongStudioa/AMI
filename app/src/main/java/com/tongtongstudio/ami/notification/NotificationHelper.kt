package com.tongtongstudio.ami.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tongtongstudio.ami.services.TrackingService

object NotificationHelper {

    private const val STOP_REQUEST_CODE = 1
    private const val RESUME_REQUEST_CODE = 2
    private const val CANCEL_REQUEST_CODE = 3

    private val flag =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_UPDATE_CURRENT

    fun clickPendingIntent(context: Context): PendingIntent {
        // Create an Intent to just the app.
        val resultIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(context, 0, resultIntent, flag)
    }

    fun pausePendingIntent(context: Context): PendingIntent {
        val stopIntent = Intent(context, TrackingService::class.java).apply {
            action = TimerNotification.ACTION_PAUSE
        }
        return PendingIntent.getService(
            context, STOP_REQUEST_CODE, stopIntent, flag
        )
    }

    fun resumePendingIntent(context: Context): PendingIntent {
        val resumeIntent = Intent(context, TrackingService::class.java).apply {
            action = TimerNotification.ACTION_START
        }
        return PendingIntent.getService(
            context, RESUME_REQUEST_CODE, resumeIntent, flag
        )
    }

    fun cancelPendingIntent(context: Context): PendingIntent {
        val cancelIntent = Intent(context, TrackingService::class.java).apply {
            action = TimerNotification.ACTION_CANCEL
        }
        return PendingIntent.getService(
            context, CANCEL_REQUEST_CODE, cancelIntent, flag
        )
    }

    fun triggerForegroundService(context: Context, action: String) {
        Intent(context, TrackingService::class.java).apply {
            this.action = action
            context.startService(this)
        }
    }
}
