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

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope