package com.tongtongstudio.ami.data.datatables

import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tongtongstudio.ami.data.MIGRATION_2_3
import com.tongtongstudio.ami.data.MIGRATION_3_5
import com.tongtongstudio.ami.data.MIGRATION_4_2
import com.tongtongstudio.ami.data.MIGRATION_5_6
import com.tongtongstudio.ami.data.ThingToDoDatabase
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ThingToDoDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate4To2_validateSchema() {
        helper.createDatabase(TEST_DB, 4)
        helper.runMigrationsAndValidate(TEST_DB, 2, false, MIGRATION_4_2)
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_validateSchema() {
        var db = helper.createDatabase(TEST_DB, 2).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.
            // Prepare for the next version.
            close()
        }
        // Re-open the database with version 3 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Test
    fun migrate2To3_correctlyTransfersTaskCompletionData() {
        // Step 1: Create database in version 2 and insert a sample task with a completion timestamp
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO task_table (title,priority, task_due_date, isCompleted, completionDate) VALUES ('Sample Task',null, null, 1, 1712000000000)")
            close()
        }

        // Step 2: Migrate to version 3
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Step 3: Check if the completion record exists in task_completions
        val cursor = db.query("SELECT * FROM task_completion_table WHERE parent_task_id = 1")
        assert(cursor.moveToFirst()) { "Data leak" } // Ensure a record exists
        val completionDate = cursor.getLong(cursor.getColumnIndexOrThrow("completionDate"))
        assertEquals(1712000000000, completionDate) // Validate the data is transferred correctly
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To5_validateSchema() {
        helper.createDatabase(TEST_DB, 3)
        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_3_5)
    }

    @Test
    fun migrate3To5_correctlyTransfersTaskCompletionData() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO task_table (title,priority, task_due_date, repetitionFrequency) 
                VALUES ('Sample Task',null, null, "1/day/"),
                ('Another Sample task', 4, 1712000000000, "2/week/2;5") 
                    """.trimIndent()
            )
            close()
        }

        // Step 2: Migrate to version 5
        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_3_5)

        // Step 3: Check if data record exists in task_table and are exact
        val taskCursor = db.query("SELECT * FROM task_table")
        assert(taskCursor.moveToFirst()) {"Problem cursor: no data in task_table"} // Ensure a record exists
        taskCursor.moveToNext()
        val dueDate = taskCursor.getLong(taskCursor.getColumnIndexOrThrow("task_due_date"))
        assertEquals(1712000000000, dueDate) // Validate the data is transferred correctly
        val taskRecurrenceId =
            taskCursor.getLong(taskCursor.getColumnIndexOrThrow("task_recurrence_id"))
        taskCursor.close()
        val recurrenceCursor =
            db.query("SELECT * FROM task_recurrence_table tr LEFT JOIN task_recurrence_days_cross_ref cr ON cr.recurrenceId = tr.recurrence_id LEFT JOIN days_of_week_table dt ON dt.day_id = cr.dayId")
        assert(recurrenceCursor.moveToFirst()) { "Problem cursor: no data in task_recurrence_table" } // Ensure a record exists
        recurrenceCursor.moveToNext()
        val recurrenceId =
            recurrenceCursor.getLong(recurrenceCursor.getColumnIndexOrThrow("recurrence_id"))
        val isActive = recurrenceCursor.getInt(recurrenceCursor.getColumnIndexOrThrow("is_active"))
        assert(taskRecurrenceId == recurrenceId) { "Problem with foreign key : task_recurrence_id ($taskRecurrenceId) not the same as recurrence_id ($recurrenceId)" }
        assert(isActive == 1)
        // TODO: test if days are well represented in database (2 and 5 for tuesday and friday (?))
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6_validateSchema() {
        helper.createDatabase(TEST_DB, 5)
        helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(TEST_DB, 2).apply {
            close()
        }

        // Open latest version of the database. Room validates the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ThingToDoDatabase::class.java,
            TEST_DB
        ).addMigrations(MIGRATION_4_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_5)
            .addMigrations(MIGRATION_5_6)
            .build().apply {
                openHelper.writableDatabase.close()
            }
    }


}
