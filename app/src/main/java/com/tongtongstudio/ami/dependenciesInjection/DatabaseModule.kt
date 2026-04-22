package com.tongtongstudio.ami.dependenciesInjection

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.tongtongstudio.ami.data.MIGRATION_2_3
import com.tongtongstudio.ami.data.MIGRATION_3_5
import com.tongtongstudio.ami.data.MIGRATION_4_2
import com.tongtongstudio.ami.data.MIGRATION_5_6
import com.tongtongstudio.ami.data.ThingToDoDatabase
import com.tongtongstudio.ami.domain.usecase.ScheduleAssessmentUseCase
import com.tongtongstudio.ami.domain.usecase.ScheduleRemindersUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        .addMigrations(MIGRATION_3_5)
        .addMigrations(MIGRATION_5_6)
        .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
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

    @Provides
    fun provideRecurrenceInfoDao(db: ThingToDoDatabase) = db.recurrenceInfoDao()

    @Provides
    fun provideTaskCompletionDao(db: ThingToDoDatabase) = db.taskCompletionDao()
    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())

    @Provides
    fun provideScheduleRemindersUseCase(
        @ApplicationContext context: Context
    ) = ScheduleRemindersUseCase(context)

    @Provides
    fun provideScheduleAssessmentsUseCase(
        @ApplicationContext context: Context
    ) = ScheduleAssessmentUseCase(context)
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope