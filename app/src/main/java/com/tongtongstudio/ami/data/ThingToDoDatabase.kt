package com.tongtongstudio.ami.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tongtongstudio.ami.data.dao.*
import com.tongtongstudio.ami.data.datatables.*
import com.tongtongstudio.ami.dependenciesInjection.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(
    entities = [Task::class,
        Project::class,
        Event::class,
        Ttd::class,
        Assessment::class,
        Reminder::class,
        Category::class],
    version = 9, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ThingToDoDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun eventDao(): EventDao
    abstract fun ttdDao(): TtdDao
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun assessmentDao(): AssessmentDao

    class Callback @Inject constructor(
        private val database: Provider<ThingToDoDatabase>,
        @ApplicationScope private val applicationScope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            val ttdDao = database.get().ttdDao()
            val categoryDao = database.get().categoryDao()
            applicationScope.launch {
                val category1 = Category(
                    "Personnel",
                    "a sample categories for all kind of task in personal life",
                )
                val category2 = Category("Job", null)
                val category3 = Category("Housework", null)
                categoryDao.insertMultipleCategories(listOf(category1, category2, category3))

                // test
                val sampleTsk = Ttd("Simple task", 2, System.currentTimeMillis())
                ttdDao.insert(sampleTsk)
                val composedTask = Ttd("Composed Task", 3, System.currentTimeMillis())
                val parentId = ttdDao.insert(composedTask)
                val subTask =
                    Ttd("Sub task", 1, System.currentTimeMillis(), parentTaskId = parentId)
                ttdDao.insert(subTask)
            }
        }
    }
}