package com.tongtongstudio.ami.data

import android.text.format.DateUtils.DAY_IN_MILLIS
import android.util.Log
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
import com.tongtongstudio.ami.data.datatables.DaysOfWeek
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.PomodoroSession
import com.tongtongstudio.ami.data.datatables.RecurringConverters
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskCompletion
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceDaysCrossRef
import com.tongtongstudio.ami.data.datatables.Unit
import com.tongtongstudio.ami.data.datatables.WorkSession
import com.tongtongstudio.ami.dependenciesInjection.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.sql.SQLException
import javax.inject.Inject
import javax.inject.Provider

val MIGRATION_4_2 = object : Migration(4, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 2. SQLite does not support ALTER COLUMN, so we recreate the table
        db.execSQL(
            "CREATE TABLE task_table_new (" +
                    "title TEXT NOT NULL, " +
                    "priority INTEGER, " +  // Keep priority as INTEGER
                    "task_due_date INTEGER, " + // Ensure type consistency
                    "startDate INTEGER DEFAULT NULL, " +
                    "deadline INTEGER DEFAULT NULL, " +
                    "description TEXT DEFAULT NULL, " +
                    "type TEXT DEFAULT NULL, " +
                    "importance INTEGER DEFAULT NULL, " +  // Ensure importance is INTEGER
                    "urgency INTEGER DEFAULT NULL, " +
                    "isDraft INTEGER NOT NULL DEFAULT 0, " +
                    "isCompleted INTEGER NOT NULL DEFAULT 0, " +
                    "completionDate INTEGER DEFAULT NULL, " +
                    "completedOnTime INTEGER DEFAULT NULL, " +
                    "estimatedWorkingTime INTEGER DEFAULT NULL, " +
                    "currentWorkingTime INTEGER DEFAULT NULL, " +
                    "isRecurring INTEGER NOT NULL DEFAULT 0, " +
                    "currentStreak INTEGER NOT NULL DEFAULT 0, " +
                    "maxStreak INTEGER NOT NULL DEFAULT 0, " +
                    "repetitionFrequency TEXT DEFAULT NULL, " +
                    "totalRepetitionCount INTEGER NOT NULL DEFAULT 0, " +
                    "timesMissed INTEGER NOT NULL DEFAULT 0, " +
                    "successCount INTEGER NOT NULL DEFAULT 0, " +
                    "comment TEXT DEFAULT NULL, " +
                    "dependencyId INTEGER DEFAULT NULL, " +
                    "skillLevel INTEGER DEFAULT NULL, " +
                    "creationDate INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000), " +
                    "task_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "categoryId INTEGER DEFAULT NULL, " +
                    "parent_task_id INTEGER DEFAULT NULL, " +
                    "FOREIGN KEY(categoryId) REFERENCES Category(category_id) ON DELETE SET NULL, " +
                    "FOREIGN KEY(parent_task_id) REFERENCES task_table(task_id) ON DELETE CASCADE)"
        )

        // 3. Copy data from old table to new table
        db.execSQL(
            "INSERT INTO task_table_new (" +
                    "title, priority, task_due_date, startDate, deadline, description, " +
                    "type, importance, urgency, isCompleted, completionDate, completedOnTime, estimatedWorkingTime, " +
                    "currentWorkingTime, isRecurring, currentStreak, maxStreak, repetitionFrequency, totalRepetitionCount, " +
                    "timesMissed, successCount, comment, dependencyId, skillLevel, creationDate, task_id, categoryId, parent_task_id" +
                    ") SELECT " +
                    "title, priority, task_due_date, startDate, deadline, description, " +
                    "type, importance, urgency, isCompleted, completionDate, completedOnTime, estimatedWorkingTime, " +
                    "currentWorkingTime, isRecurring, currentStreak, maxStreak, repetitionFrequency, totalRepetitionCount, " +
                    "timesMissed, successCount, comment, dependencyId, skillLevel, creationDate, task_id, categoryId, parent_task_id " +
                    "FROM task_table"
        )

        // 4. Remove old table and rename the new one
        db.execSQL("DROP TABLE task_table")
        db.execSQL("ALTER TABLE task_table_new RENAME TO task_table")

        /*db.execSQL("ALTER TABLE task_table ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE task_table ALTER COLUMN importance INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE task_table ALTER COLUMN priority INTEGER")
        db.execSQL("ALTER TABLE task_table ALTER COLUMN dueDate LONG")*/
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Créer la table DaysOfWeek
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS days_of_week_table (
                day_id INTEGER PRIMARY KEY NOT NULL,
                name TEXT NOT NULL
            )
            """
        )

        // Pré-remplir les jours de la semaine
        db.execSQL(
            """
            INSERT INTO days_of_week_table (day_id, name)
            VALUES (2, 'Monday'), (3, 'Tuesday'), (4, 'Wednesday'),
                   (5, 'Thursday'), (6, 'Friday'), (7, 'Saturday'), (1, 'Sunday')
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
                FOREIGN KEY (recurrenceId) REFERENCES task_recurrence_table(recurrence_id) ON DELETE CASCADE,
                FOREIGN KEY (dayId) REFERENCES days_of_week_table(day_id) ON DELETE CASCADE
            )
            """
        )

        // Créer la table TaskCompletion //DEFAULT (strftime('%s', 'now') * 1000)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_completion_table (
                parent_task_id INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                completionDate INTEGER NOT NULL,
                comment TEXT,
                emotions INTEGER,
                completion_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                FOREIGN KEY (parent_task_id) REFERENCES task_table(task_id) ON DELETE CASCADE
            )
            """
        )
        insertTaskCompletions(db)
    }

    fun insertTaskCompletions(db: SupportSQLiteDatabase) {
        // Extract completion data from task_table (assuming completion info was stored there)
        val cursor = db.query("SELECT task_id, completionDate FROM task_table WHERE completionDate IS NOT NULL")
        while (cursor.moveToNext()) {
            val taskId = cursor.getLong(0)
            val completionDate = cursor.getLong(1)

            // Insert into task_completions table
            db.execSQL(
                "INSERT INTO task_completion_table (parent_task_id, isCompleted, completionDate) VALUES ($taskId,1, $completionDate)"
            )
        }
        cursor.close()
    }
}

val MIGRATION_3_5 = object : Migration(3, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE new_task_table (
                title TEXT NOT NULL,
                priority INTEGER,
                task_due_date INTEGER,
                startDate INTEGER,
                deadline INTEGER,
                description TEXT,
                type TEXT,
                status TEXT DEFAULT "not_started",
                importance INTEGER,
                urgency INTEGER,
                isDraft INTEGER NOT NULL DEFAULT 0,
                estimatedEmotions INTEGER NOT NULL DEFAULT 1,
                estimatedWorkingTime INTEGER,
                skillLevel INTEGER,
                creationDate INTEGER NOT NULL,
                dependency_task_id INTEGER,
                task_recurrence_id INTEGER,
                category_id INTEGER,
                parent_task_id INTEGER,
                task_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES Category(category_id) ON DELETE SET NULL,
                FOREIGN KEY(parent_task_id) REFERENCES task_table(task_id) ON DELETE CASCADE,
                FOREIGN KEY(task_recurrence_id) REFERENCES task_recurrence_table(recurrence_id) ON DELETE SET NULL,                
                FOREIGN KEY(dependency_task_id) REFERENCES task_table(task_id) ON DELETE SET NULL
            )
                """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO new_task_table (
                title, priority, task_due_date, startDate, deadline, description, type,
                importance, urgency, isDraft,estimatedWorkingTime, skillLevel,
                creationDate, category_id, parent_task_id, task_id
            )
            SELECT title, priority, task_due_date, startDate, deadline, description, type,
                importance, urgency, isDraft, estimatedWorkingTime, skillLevel,
                creationDate, category_id, parent_task_id, task_id
            FROM task_table
            """.trimIndent()
        )

        // Insert recurrence infos from old_task_table infos into new_task_table
        insertTaskRecurrences(db)

        // Supprimer l'ancienne table task_table
        db.execSQL("DROP TABLE task_table")

        // Renommer la nouvelle table
        db.execSQL("ALTER TABLE task_table_new RENAME TO task_table")
    }

    fun insertTaskRecurrences(db: SupportSQLiteDatabase) {
        // Récupérer toutes les tâches qui ont une récurrence
        val cursor = db.query("SELECT task_id, task_due_date, repetitionFrequency FROM task_table WHERE recurrence IS NOT NULL")
        // TODO: Verify repetitionFrequency is not null
        while (cursor.moveToNext()) {
            val taskId = cursor.getLong(cursor.getColumnIndexOrThrow("task_id"))
            val startDate = cursor.getLong(cursor.getColumnIndexOrThrow("task_due_date"))
            val recurrenceString = cursor.getString(cursor.getColumnIndexOrThrow("repetitionFrequency"))

            // Vérifier si le format est correct avant de continuer
            val recurrenceParts = recurrenceString.split("/")
            if (recurrenceParts.size < 3) continue

            val interval = recurrenceParts[0].toIntOrNull() ?: continue
            val frequency = recurrenceParts[1] // "DAILY", "WEEKLY", etc.
            val daysOfWeek = recurrenceParts[2].split(",").mapNotNull { it.toIntOrNull() } // Liste de jours

            // Insérer dans TaskRecurrence
            db.execSQL(
                "INSERT INTO task_recurrence_table (frequency, interval, start_date, is_active) " +
                        "VALUES (?, ?, ?, 1)",
                arrayOf(frequency, interval, startDate)
            )

            // Récupérer l'ID de la nouvelle recurrence
            val recurrenceCursor = db.query("SELECT last_insert_rowid()")
            recurrenceCursor.moveToFirst()
            val recurrenceId = recurrenceCursor.getLong(0)
            recurrenceCursor.close()

            // Associer les jours de la semaine à la récurrence
            for (dayId in daysOfWeek) {
                db.execSQL(
                    "INSERT INTO task_recurrence_days_cross_ref (recurrenceId, dayId) VALUES (?, ?)",
                    arrayOf(recurrenceId, dayId)
                )
            }
            // Update new_task_table
            db.execSQL("UPDATE new_task_table SET task_recurrence_id = $recurrenceId WHERE task_id = $taskId")
        }
        cursor.close()
    }

}

@Database(
    entities = [
        Task::class,
        Assessment::class,
        Reminder::class,
        Category::class, Unit::class,
        WorkSession::class,
        PomodoroSession::class,
        TaskRecurrence::class, TaskRecurrenceDaysCrossRef::class,
        DaysOfWeek::class, TaskCompletion::class],
    version = 5,  exportSchema = true
)//autoMigrations = [AutoMigration(4,2), AutoMigration(2,3)],
@TypeConverters(RecurringConverters::class)
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
            //configureSQLitePragmas(db)
            val taskDao = database.get().taskDao()
            val categoryDao = database.get().categoryDao()
            val assessmentDao = database.get().assessmentDao()

            applicationScope.launch {
                insertInitialTasks(taskDao, categoryDao, assessmentDao)
            }
        }

        private fun configureSQLitePragmas(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("PRAGMA auto_vacuum = INCREMENTAL")
                db.execSQL("PRAGMA journal_mode = WAL")
                db.execSQL("PRAGMA synchronous = NORMAL")
                db.execSQL("PRAGMA foreign_keys = ON")
            } catch (e: SQLException) {
                Log.e("Database", "Error configuring SQLite pragmas", e)
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