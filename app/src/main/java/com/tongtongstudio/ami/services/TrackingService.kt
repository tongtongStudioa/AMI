package com.tongtongstudio.ami.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.notification.NotificationHelper
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_CANCEL
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_PAUSE
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_START
import com.tongtongstudio.ami.notification.TimerNotification.Companion.CHANNEL_ID_TIMER
import com.tongtongstudio.ami.notification.TimerNotification.Companion.CHANNEL_NAME_TIMER
import com.tongtongstudio.ami.notification.TimerNotification.Companion.NOTIFICATION_TIMER_ID
import com.tongtongstudio.ami.timer.TimerStopWatch
import com.tongtongstudio.ami.timer.TimerType
import com.tongtongstudio.ami.timer.TrackingConstants.ACTION_TIMER_TYPE_COUNTDOWN
import com.tongtongstudio.ami.timer.TrackingConstants.ACTION_TIMER_TYPE_STOPWATCH
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class TrackingService : LifecycleService() {

    // Sonification
    private lateinit var soundPool: SoundPool
    private var soundRestOver: Int = 0
    private var soundWorkOver: Int = 0
    private var soundPomodoroOver: Int = 0

    // timer and stopwatch
    private var isFirstRun = true
    var serviceKilled = false
    private var taskName: String? = null
    private lateinit var timer: TimerStopWatch
    private var timeStarted = 0L

    // notification
    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    private lateinit var curNotificationBuilder: NotificationCompat.Builder

    companion object {
        val timeInMillis = MutableLiveData<Long>()
        val timeWorkedInMillis = MutableLiveData<Long>()
        val isRunning = MutableLiveData<Boolean>()
        val timerType = MutableLiveData<TimerType>()
    }

    override fun onCreate() {
        super.onCreate()
        // to update notification
        curNotificationBuilder = baseNotificationBuilder

        // to initialize values
        postInitialValues()

        // set up sonification
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            SoundPool(1, AudioManager.STREAM_MUSIC, 0)
        }
        soundRestOver = soundPool.load(this, R.raw.rest_time_over, 1)
        soundPomodoroOver = soundPool.load(this, R.raw.pomodoro_sessions_over, 1)
        soundWorkOver = soundPool.load(this, R.raw.work_time_over, 1)

        timer = TimerStopWatch(object : TimerStopWatch.TimerListener {
            override fun onTick(timeDisplayed: Long, timeWorked: Long) {
                timeInMillis.postValue(timeDisplayed)
                timeWorkedInMillis.postValue(timeWorked)
            }

            override fun onWorkSessionFinish() {
                playSound(R.raw.work_time_over)
            }

            override fun onRestSessionFinish() {
                playSound(R.raw.rest_time_over)
            }

            override fun onPomodoroSessionsFinish() {
                playSound(R.raw.pomodoro_sessions_over)
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            //Log.d(javaClass.simpleName, "Intend received: ${it.action}")
            when (it.action) {
                ACTION_START -> {
                    setPauseNotifAction()
                    if (isFirstRun)
                        taskName = it.getStringExtra("task_title")
                    startOrResumeTracking()
                }
                ACTION_PAUSE -> {
                    pauseTracking()
                    setResumeNotifAction()
                }
                ACTION_CANCEL -> {
                    resetTracking()
                    cancelService()
                }
                ACTION_TIMER_TYPE_COUNTDOWN -> {
                    changeTimerType(TimerType.COUNTDOWN)
                }
                ACTION_TIMER_TYPE_STOPWATCH -> {
                    changeTimerType(TimerType.STOPWATCH)
                }
                else -> {}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun changeTimerType(type: TimerType) {
        timer.updateType(type)
        timerType.postValue(type)
        isRunning.postValue(false)
    }

    private fun resetTracking() {
        timer.reset()
        isRunning.postValue(false)
        //timeWorkedInMillis.postValue(0L)
    }

    private fun startOrResumeTracking() {
        timer.start()
        isRunning.postValue(true)
        if (isFirstRun) {
            startForegroundService()
            isFirstRun = false
        }
    }

    private fun pauseTracking() {
        timer.pause()
        isRunning.postValue(false)
    }

    private fun cancelService() {
        resetTracking()
        serviceKilled = true
        isFirstRun = true
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    private fun playSound(soundId: Int) {
        soundPool.play(soundId, 0.6F, 0.6F, 1, 0, 1.1F)
    }

    private fun postInitialValues() {
        isRunning.postValue(false)
        timerType.value = TimerType.STOPWATCH
        timeInMillis.postValue(0L)
        timeWorkedInMillis.postValue(0L)
    }

    @SuppressLint("RestrictedApi")
    private fun setResumeNotifAction() {
        curNotificationBuilder.mActions.removeAt(0)
        curNotificationBuilder.mActions.add(
            0,
            NotificationCompat.Action(
                0,
                getString(R.string.action_resume_titile),
                NotificationHelper.resumePendingIntent(this)
            )
        )
        notificationManager.notify(NOTIFICATION_TIMER_ID, curNotificationBuilder.build())
    }

    @SuppressLint("RestrictedApi")
    private fun setPauseNotifAction() {
        curNotificationBuilder.mActions.removeAt(0)
        curNotificationBuilder.mActions.add(
            0,
            NotificationCompat.Action(
                0,
                getString(R.string.action_pause_title),
                NotificationHelper.pausePendingIntent(this)
            )
        )
        notificationManager.notify(NOTIFICATION_TIMER_ID, curNotificationBuilder.build())
    }

    /**
     * Show notification on when tracking time is running and update time displayed in notification with observer on time.
     */
    private fun startForegroundService() {
        // post notification
        createNotificationChannel()
        startForeground(NOTIFICATION_TIMER_ID, baseNotificationBuilder.build())

        timeInMillis.observe(this) {
            val contentText = getString(
                R.string.tracking_notification_text,
                taskName,
                TrackingTimeUtility.getFormattedWorkTime(
                    it,
                    timerType.value!!
                )
            )
            if (!serviceKilled) {
                val notification = curNotificationBuilder
                    .setContentText(contentText)
                notificationManager.notify(NOTIFICATION_TIMER_ID, notification.build())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_TIMER,
                CHANNEL_NAME_TIMER,
                IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}