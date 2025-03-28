package com.tongtongstudio.ami.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATIONS_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table "tasks_new"
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

        // Suppress old table "task_table"
        db.execSQL("DROP TABLE task_table")

        // Rename new table
        db.execSQL("ALTER TABLE task_table_new RENAME TO task_table")
    }
}