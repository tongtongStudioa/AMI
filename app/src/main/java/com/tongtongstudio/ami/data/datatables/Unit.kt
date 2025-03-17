package com.tongtongstudio.ami.data.datatables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Unit(
    val name: String,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "unit_id") val id: Long = 0
)