package com.tongtongstudio.ami.data.datatables

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tongtongstudio.ami.data.MIGRATION_2_3
import com.tongtongstudio.ami.data.ThingToDoDatabase
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper = ThingToDoDatabase::class.java.canonicalName?.let {
        MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
            it
        )
    }

    @Test
    fun migrateFrom2To3() {
        // Créez une base de données en version 1
        val db: SupportSQLiteDatabase?

        // Migrer vers la version 2
        db = helper?.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3) ?: return

        // Vérifiez que les données sont correctement migrées
        val cursor = db.query("SELECT * FROM tasks WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(cursor.getString(cursor.getColumnIndex("name")), "Test Task")
        assertEquals(cursor.getInt(cursor.getColumnIndex("autoReschedule")), 0)
        assertEquals(cursor.getInt(cursor.getColumnIndex("emotion")), 0)
        cursor.close()
    }


    @Test
    fun testMigration2To3_dataIntegrity() {
        // Créer une base de données en version 2
        val db: SupportSQLiteDatabase?

        // Migrer vers la version 3
        db = helper?.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3) ?: return

        // Vérifier que les données sont toujours présentes après la migration
        db.query("SELECT * FROM task_table WHERE task_id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Test Task", cursor.getString(cursor.getColumnIndex("title")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndex("priority")))
        }
    }


}
