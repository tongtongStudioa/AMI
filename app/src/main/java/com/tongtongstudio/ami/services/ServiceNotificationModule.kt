package com.tongtongstudio.ami.timer

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.notification.NotificationHelper
import com.tongtongstudio.ami.notification.TimerNotification
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    fun provideBaseNotificationBuilder(
        @ApplicationContext context: Context
    ) = NotificationCompat.Builder(context, TimerNotification.CHANNEL_ID_TIMER)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_small_notif)
        .setContentTitle(context.getString(R.string.tracking_notification_title))
        .addAction(
            0,
            context.getString(R.string.action_pause_title),
            NotificationHelper.pausePendingIntent(context)
        )
        .addAction(
            0,
            context.getString(R.string.action_cancel_title),
            NotificationHelper.cancelPendingIntent(context)
        )
        .setContentIntent(NotificationHelper.clickPendingIntent(context))

    @ServiceScoped
    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}