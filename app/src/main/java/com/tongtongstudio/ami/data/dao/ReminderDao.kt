package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.datatables.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM Reminder WHERE parent_id = :taskId ORDER BY reminder_id ASC")
    fun getTaskReminders(taskId: Long): Flow<MutableList<Reminder>>

    @Query("SELECT * FROM Reminder WHERE reminder_id = :id LIMIT 1")
    suspend fun get(id: Long): Reminder

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)
}