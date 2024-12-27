package com.tongtongstudio.ami.dependenciesInjection

import android.app.Application
import androidx.room.Room
import com.tongtongstudio.ami.data.MIGRATION_2_3
import com.tongtongstudio.ami.data.MIGRATION_4_2
import com.tongtongstudio.ami.data.ThingToDoDatabase
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
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        app: Application,
        callback: ThingToDoDatabase.Callback
    ) = Room.databaseBuilder(app, ThingToDoDatabase::class.java, "thing_to_do_database")
        .addMigrations(MIGRATION_4_2)
        .addMigrations(MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .addCallback(callback)
        .build()

    @Provides
    fun provideTtdDao(db: ThingToDoDatabase) = db.taskDao()

    @Provides
    fun provideReminderDao(db: ThingToDoDatabase) = db.reminderDao()

    @Provides
    fun provideCategoryDao(db: ThingToDoDatabase) = db.categoryDao()

    @Provides
    fun provideWorkSessionDao(db: ThingToDoDatabase) = db.workSession()

    @Provides
    fun provideAssessmentDao(db: ThingToDoDatabase) = db.assessmentDao()

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope