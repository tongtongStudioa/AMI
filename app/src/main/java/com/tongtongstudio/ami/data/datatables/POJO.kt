package com.tongtongstudio.ami.data.datatables

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

const val PATTERN_FORMAT_DATE = "E dd/MM"

enum class Nature { PROJECT, TASK }

@Parcelize
data class ThingToDo(
    @Embedded
    val mainTask: Task,
    @Relation(parentColumn = "task_id", entityColumn = "parent_task_id", entity = Task::class)
    val subTasks: List<Task>,
    @Relation(parentColumn = "categoryId", entityColumn = "category_id", entity = Category::class)
    val category: Category?,
    @Relation(parentColumn = "task_id", entityColumn = "parent_id", entity = Reminder::class)
    val reminders: List<Reminder>

) : Parcelable {
    fun isProject(): Boolean {
        return mainTask.type == Nature.PROJECT.name || subTasks.isNotEmpty()
    }

    fun getNbSubTasksCompleted(): Int = subTasks.count { it.isCompleted }
    fun getNbSubTasks(): Int = subTasks.size
}

/**
 * Class with task completed info to analyse productivity (number of task achieved in a period of time).
 */
data class TtdAchieved(
    val completionDate: Long,
    val completedCount: Float
)

/**
 * Class with recurring task info for max and min streak.
 */
data class TtdStreakInfo(
    val title: String?,
    val streakInfo: Int?
)

/**
 * Describe type of objective targetGoal to help tracking progress.
 */
enum class AssessmentType { QUANTITY, DURATION, BOOLEAN }

data class TimeWorkedDistribution(val title: String?, val totalTimeWorked: Long?)

data class CategoryTasks(
    @Embedded
    val category: Category,
    @Relation(entity = Task::class, parentColumn = "category_id", entityColumn = "categoryId")
    val tasks: List<Task>
)

