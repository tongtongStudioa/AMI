package com.tongtongstudio.ami.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import com.tongtongstudio.ami.data.datatables.TaskCompletion

@Dao
interface TaskCompletionDao {

    @Insert
    suspend fun insert(completion: TaskCompletion)

    @Delete
    suspend fun delete(completion: TaskCompletion)

    @Update
    suspend fun update(completion: TaskCompletion)
}