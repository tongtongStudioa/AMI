package com.tongtongstudio.ami.dependenciesInjection

import android.app.Application
import androidx.room.Room
import com.tongtongstudio.ami.data.ThingToDoDatabase1
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(
        app: Application,
        callback: ThingToDoDatabase1.Callback
    ) = Room.databaseBuilder(app, ThingToDoDatabase1::class.java, "thing_to_do_database")
        .fallbackToDestructiveMigration()
        .addCallback(callback)
        .build()

    @Provides
    fun provideTaskDao(db: ThingToDoDatabase1) = db.taskDao()

    @Provides
    fun provideEventDao(db: ThingToDoDatabase1) = db.eventDao()

    @Provides
    fun provideProjectDao(db: ThingToDoDatabase1) = db.projectDao()

    @Provides
    fun provideTtdDao(db: ThingToDoDatabase1) = db.ttdDao()

    @Provides
    fun provideReminderDao(db: ThingToDoDatabase1) = db.reminderDao()

    @Provides
    fun provideCategoryDao(db: ThingToDoDatabase1) = db.categoryDao()

    @Provides
    fun provideAssessmentDao(db: ThingToDoDatabase1) = db.assessmentDao()

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope