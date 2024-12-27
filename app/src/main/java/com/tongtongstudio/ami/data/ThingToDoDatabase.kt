package com.tongtongstudio.ami.data

import android.text.format.DateUtils.DAY_IN_MILLIS
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tongtongstudio.ami.data.dao.AssessmentDao
import com.tongtongstudio.ami.data.dao.CategoryDao
import com.tongtongstudio.ami.data.dao.ReminderDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.dao.WorkSessionDao
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.AssessmentType
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Completion
import com.tongtongstudio.ami.data.datatables.Converters
import com.tongtongstudio.ami.data.datatables.DaysOfWeek
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.PomodoroSession
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceDaysCrossRef
import com.tongtongstudio.ami.data.datatables.Unit
import com.tongtongstudio.ami.data.datatables.WorkSession
import com.tongtongstudio.ami.dependenciesInjection.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

val MIGRATION_4_2 = object : Migration(4, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task_table ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_table ALTER COLUMN importance INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE task_table ALTER COLUMN priority INTEGER")
        db.execSQL("ALTER TABLE task_table ALTER COLUMN dueDate LONG")
    }
}

val MIGRATION_2_3 = object : Migration(4, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Créer la table DaysOfWeek
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS days_of_week_table (
                dayId INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL
            )
            """
        )

        // Pré-remplir les jours de la semaine
        db.execSQL(
            """
            INSERT INTO days_of_week_table (dayId, name)
            VALUES (1, 'Monday'), (2, 'Tuesday'), (3, 'Wednesday'),
                   (4, 'Thursday'), (5, 'Friday'), (6, 'Saturday'), (7, 'Sunday')
            """
        )

        db.execSQL(
            """
            CREATE TABLE task_recurrence_table (
                frequency TEXT NOT NULL,
                interval INTEGER NOT NULL,
                start_date INTEGER,
                end_date INTEGER,
                is_active INTEGER NOT NULL DEFAULT 1,
                occurrence_limit INTEGER,
                recurrence_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
                )
            """.trimIndent()
        )

        // Créer la table TaskRecurrenceDaysCrossRef
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_recurrence_days_cross_ref (
                recurrenceId INTEGER NOT NULL,
                dayId INTEGER NOT NULL,
                PRIMARY KEY (recurrenceId, dayId),
                FOREIGN KEY (recurrenceId) REFERENCES recurrence_task_table(recurrence_id) ON DELETE CASCADE,
                FOREIGN KEY (dayId) REFERENCES days_of_week_table(dayId) ON DELETE CASCADE
            )
            """
        )

        // Créer la table TaskCompletion
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_completion_table (
                parent_task_id INTEGER NOT NULL,
                completionDate INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                comment TEXT,
                emotions INTEGER,
                completion_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                FOREIGN KEY (parent_task_id) REFERENCES task_table(task_id) ON DELETE CASCADE
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE tasks_new (
                title TEXT NOT NULL,
                priority INTEGER,
                task_due_date INTEGER,
                startDate INTEGER,
                deadline INTEGER,
                description TEXT,
                type TEXT,
                importance INTEGER,
                urgency INTEGER,
                isDraft INTEGER NOT NULL DEFAULT 0,
                estimatedEmotions INTEGER NOT NULL DEFAULT 1,
                estimatedWorkingTime INTEGER,
                skillLevel INTEGER,
                creationDate INTEGER NOT NULL,
                dependency_task_id INTEGER,
                recurrence_infos_id INTEGER,
                categoryId INTEGER,
                parent_task_id INTEGER,
                task_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                FOREIGN KEY(dependency_task_id) REFERENCES task_table(task_id) ON DELETE SET NULL,
                FOREIGN KEY(categoryId) REFERENCES category_table(category_id) ON DELETE SET NULL,
                FOREIGN KEY(parent_task_id) REFERENCES task_table(task_id) ON DELETE CASCADE,
                FOREIGN KEY(task_recurrence_id) REFERENCES task_recurrence_table(recurrence_id) ON DELETE SET NULL
            )
                """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO task_table_new (
                title, priority, task_due_date, startDate, deadline, description, type,
                importance, urgency, isDraft, estimatedEmotions,estimatedWorkingTime, skillLevel,
                creationDate, dependency_task_id, task_recurrence_id,
                categoryId, parent_task_id, task_id
            )
            SELECT title, priority, task_due_date, startDate, deadline, description, type,
                importance, urgency, isDraft, 1 AS estimatedEmotions, estimatedWorkingTime, skillLevel,
                creationDate, dependency_task_id, task_recurrence_id, categoryId, parent_task_id, task_id
            FROM task_table
            """.trimIndent()
        )

        // Supprimer l'ancienne table task_table
        db.execSQL("DROP TABLE task_table")

        // Renommer la nouvelle table
        db.execSQL("ALTER TABLE task_table_new RENAME TO task_table")
    }
}

@Database(
    entities = [
        Task::class,
        TaskRecurrence::class,
        TaskRecurrenceDaysCrossRef::class,
        DaysOfWeek::class,
        Completion::class,
        Assessment::class,
        Reminder::class,
        Category::class, Unit::class,
        WorkSession::class, PomodoroSession::class],
    version = 2, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ThingToDoDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun workSession(): WorkSessionDao

    open class Callback @Inject constructor(
        private val database: Provider<ThingToDoDatabase>,
        @ApplicationScope private val applicationScope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val taskDao = database.get().taskDao()
            val categoryDao = database.get().categoryDao()
            val assessmentDao = database.get().assessmentDao()

            applicationScope.launch {
                insertInitialTasks(taskDao, categoryDao, assessmentDao)
                /*val category1 = Category(
                    "Personnel",
                    "a sample categories for all kind of task in personal life",
                )
                val category2 = Category("Job", null)
                val category3 = Category("Housework", null)
                categoryDao.insertMultipleCategories(listOf(category1, category2, category3))

                // test
                val sampleTsk = Task("Simple task", 2, System.currentTimeMillis())
                ttdDao.insert(sampleTsk)
                val composedTask = Task("Composed Task", 3, System.currentTimeMillis())
                val parentId = ttdDao.insert(composedTask)
                val subTask =
                    Task("Sub task", 1, System.currentTimeMillis(), parentTaskId = parentId)
                ttdDao.insert(subTask)*/
            }
        }

        private val initialCategories = listOf(
            Category(title = "Work", description = "Tasks related to work."),
            Category(title = "Personal", description = "Personal tasks and reminders."),
            Category(title = "Fitness", description = "Health and fitness activities.")
        )

        private val initialObjectives = listOf(
            Assessment(
                title = "Run 10km",
                targetGoal = 10F,
                unit = "Km",
                type = AssessmentType.QUANTITY.name,
                dueDate = System.currentTimeMillis() + 30 * DAY_IN_MILLIS
            ),
            Assessment(
                title = "Learn French",
                targetGoal = 100F,
                unit = "Hours",
                description = "Work 100h on learning french to prepare trip in Paris !",
                type = AssessmentType.QUANTITY.name,
                dueDate = System.currentTimeMillis() + 10 * DAY_IN_MILLIS
            )

        )

        private fun initialTasks(listId: List<Long>) = listOf(
            Task(
                title = "Attend Team Meeting",
                priority = 1,
                dueDate = System.currentTimeMillis(),
                description = "Attend the regular team meeting to discuss project updates.",
                type = Nature.TASK.name,
                categoryId = null,
                parentTaskId = null
            ),
            Task(
                title = "Complete Project Report",
                priority = 2,
                dueDate = System.currentTimeMillis() + 3 * DAY_IN_MILLIS, // 3 days from now
                description = "Prepare and complete the report on the current project status.",
                type = Nature.TASK.name,
                categoryId = listId[0],
                parentTaskId = null
            ),
            Task(
                title = "Morning Exercise",
                priority = 3,
                dueDate = System.currentTimeMillis() + 7 * DAY_IN_MILLIS, // 1 week from now
                description = "Complete a 30-minute morning exercise routine.",
                type = Nature.TASK.name,
                categoryId = listId[2],
                parentTaskId = null
            ),
            Task(
                title = "Study for Exams",
                priority = 4,
                dueDate = System.currentTimeMillis() + 10 * DAY_IN_MILLIS, // 10 days from now
                description = "Study for the upcoming exams in mathematics and science.",
                type = Nature.TASK.name,
                categoryId = listId[1],
                parentTaskId = null
            ),
            Task(
                title = "Buy Groceries",
                priority = 5,
                dueDate = System.currentTimeMillis(),
                description = "Buy essential groceries for the week.",
                type = Nature.TASK.name,
                categoryId = null,
                parentTaskId = null
            )
        )

        // Function to populate the database with initial tasks
        private suspend fun insertInitialTasks(
            taskDao: TaskDao,
            categoryDao: CategoryDao,
            assessmentDao: AssessmentDao
        ) {
            val arrayCategoriesId = arrayListOf<Long>()
            initialCategories.forEach { category ->
                val id: Long = categoryDao.insert(category)
                arrayCategoriesId.add(id)
            }
            initialTasks(arrayCategoriesId).forEach { task ->
                taskDao.insert(task)
            }

            initialObjectives.forEach { objective ->
                assessmentDao.insert(objective)
            }
        }
    }
}