package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Category(
    @ColumnInfo(name = "category_title")
    val title: String,
    val description: String?,
    val color: Int? = null,
    @ColumnInfo(name = "parent_category_id")
    val parentCategoryId: Long? = null,
    @ColumnInfo(name = "category_id")
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : Parcelable