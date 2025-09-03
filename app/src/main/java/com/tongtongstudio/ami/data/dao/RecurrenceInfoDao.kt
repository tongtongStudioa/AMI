package com.tongtongstudio.ami.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceInfoDao {

    @Transaction
    @Query("SELECT * " +
            "FROM task_recurrence_table tr " +
            "LEFT JOIN task_recurrence_days_cross_ref as tc ON tr.recurrence_id = tc.recurrenceId " +
            "LEFT JOIN days_of_week_table dw ON tc.dayId = dw.day_id " +
            "WHERE recurrence_id =:recurrenceId")
    fun getRecurrenceInfoById(recurrenceId: Long): Flow<TaskRecurrence>

    @Insert
    suspend fun insertRecurringInfo(recurringInfo: TaskRecurrence)

    @Delete
    suspend fun deleteRecurringInfo(recurringInfo: TaskRecurrence)

    @Update
    suspend fun updateRecurringInfo(recurringInfo: TaskRecurrence)
}