package com.tongtongstudio.ami.data.datatables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WorkSession(
    val parentTaskId: Long,
    val duration: Long,
    val comment: String?,
    val date: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "work_session_id") val id: Long = 0
)