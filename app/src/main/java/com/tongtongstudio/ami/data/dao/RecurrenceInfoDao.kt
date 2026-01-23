package com.tongtongstudio.ami.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tongtongstudio.ami.data.datatables.DaysOfWeek
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceDaysCrossRef
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays

@Dao
interface RecurrenceInfoDao {

    @Transaction
    @Query("SELECT * " +
            "FROM task_recurrence_table tr " +
            "WHERE recurrence_id =:recurrenceId")
    suspend fun getTaskRecurrenceById(recurrenceId: Long): TaskRecurrence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringInfo(recurringInfo: TaskRecurrence): Long

    @Delete
    suspend fun deleteRecurringInfo(recurringInfo: TaskRecurrence)

    @Update
    suspend fun updateRecurringInfo(recurringInfo: TaskRecurrence)
    @Insert(entity = DaysOfWeek::class)
    suspend fun insertDayOfWeek(day: DaysOfWeek)

    @Query("SELECT * FROM days_of_week_table " +
            "WHERE day_id IN (:ids) ")
    suspend fun getDaysOfWeeks(ids: List<Int>): List<DaysOfWeek>

    @Query("SELECT * FROM task_recurrence_days_cross_ref " +
            "WHERE recurrenceId = :recurrenceId AND dayId = :dayId")
    suspend fun getCrossRefDay(recurrenceId: Long,dayId: Long): TaskRecurrenceDaysCrossRef?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefForDays(recurrenceAndDaysCrossRef: List<TaskRecurrenceDaysCrossRef>)
    @Query("SELECT * FROM task_recurrence_table " +
            "WHERE recurrence_id = :taskRecurrenceId"
    )
    suspend fun getTaskRecurrenceWithDays(taskRecurrenceId: Long): TaskRecurrenceWithDays
}