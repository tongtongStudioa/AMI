package com.tongtongstudio.ami.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tongtongstudio.ami.data.dao.EventDao
import com.tongtongstudio.ami.data.dao.ProjectDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.datatables.Event
import com.tongtongstudio.ami.data.datatables.Project
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.dependenciesInjection.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Task::class, Project::class, Event::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ThingToDoDatabase1 : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun eventDao(): EventDao

    class Callback @Inject constructor(
        private val database: Provider<ThingToDoDatabase1>,
        @ApplicationScope private val applicationScope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            val taskDao = database.get().taskDao()
            val projectDao = database.get().projectDao()
            val eventDao = database.get().eventDao()

            /*applicationScope.launch {
                taskDao.insert(Task("Go faire les courses", 4))
                val projectId = projectDao.insert(Project("Exam Mathematics", 8, nb_sub_task = 1))
                eventDao.insert(Event("Birthday brother", 7))
                taskDao.insert(Task("sub task", 5, projectId = projectId))
            }*/
        }
    }
}

/*
abstract class ThingToDoDatabase2 : RoomDatabase() {

    class Callback @Inject constructor(
        private val database: Provider<ThingToDoDatabase1>,
        @ApplicationScope private val applicationScope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            val taskDao = database.get().taskDao()
            val projectDao = database.get().projectDao()
            val eventDao = database.get().eventDao()

            /*applicationScope.launch {
                taskDao.insert(Task("Go faire les courses", 4))
                val projectId = projectDao.insert(Project("Exam Mathematics", 8, nb_sub_task = 1))
                eventDao.insert(Event("Birthday brother", 7))
                taskDao.insert(Task("sub task", 5, projectId = projectId))
            }*/
        }
    }
}
 */