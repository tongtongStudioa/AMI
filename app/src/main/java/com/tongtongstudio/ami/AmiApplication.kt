package com.tongtongstudio.ami

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.notification.AssessmentWorker
import com.tongtongstudio.ami.notification.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class AmiApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: AppWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

class AppWorkerFactory @Inject constructor(
    private val repository: Repository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when (workerClassName) {

            ReminderWorker::class.java.name ->
                ReminderWorker(appContext, workerParameters, repository)

            AssessmentWorker::class.java.name ->
                AssessmentWorker(appContext, workerParameters, repository)

            else -> null
        }
    }
}