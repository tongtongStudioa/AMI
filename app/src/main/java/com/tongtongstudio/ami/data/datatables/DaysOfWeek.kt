package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "days_of_week_table")
data class DaysOfWeek(
    @PrimaryKey(autoGenerate = false) // days are fixed
    val dayId: Int, // 1 (Monday) to 7 (Sunday)
    val name: String // "Monday", "Tuesday", etc.
) : Parcelable