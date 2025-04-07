package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.DateFormat

@Parcelize
@Entity(
    tableName = "task_completion_table", foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["task_id"],
            childColumns = ["parent_task_id"],
            onDelete = CASCADE
        )
    ]
)
data class TaskCompletion(
    @ColumnInfo(name = "parent_task_id")
    val taskId: Long,
    val isCompleted: Boolean = false,
    val completionDate: Long = System.currentTimeMillis(),
    val comment: String? = null,
    val emotions: Int? = 1,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "completion_id")
    val id: Long = 0
) : Parcelable {
    /**
     * This function decide if a task is completed on time or not.
     * It compares completionDate and dueDate or completionDate and deadline if it was define
     * @return boolean
     */
    private fun hasBeenCompletedOnTime(
        completionDate: Long,
        dueDate: Long,
        deadline: Long?
    ): Boolean {
        return completionDate < dueDate || (deadline != null && completionDate < deadline)
    }

    fun getCompletionDateFormatted(): String {
        return DateFormat.getDateInstance().format(completionDate)
    }
}