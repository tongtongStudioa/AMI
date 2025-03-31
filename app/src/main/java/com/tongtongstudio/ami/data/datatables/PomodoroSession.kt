package com.tongtongstudio.ami.data.datatables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PomodoroSession(
    val name: String,
    val workingDuration: Long = 30 * 60 * 1000,
    val restDuration: Long = 15 * 60 * 1000,
    val workSessionsCount: Int = 4,
    val specialMsg: String?,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "work_session_id") val id: Long = 0
)