package com.tongtongstudio.ami.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tongtongstudio.ami.data.datatables.WorkSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workSession: WorkSession): Long

    @Update
    suspend fun update(workSession: WorkSession)

    @Delete
    suspend fun delete(workSession: WorkSession)

    @Query("SELECT * FROM worksession WHERE parentTaskId = :taskId")
    fun getWorkSessions(taskId: Long): Flow<List<WorkSession>>

    @Query("SELECT SUM(duration) FROM worksession WHERE parentTaskId = :taskId")
    fun getTaskTimeWorked(taskId: Long): Flow<Long>
}
