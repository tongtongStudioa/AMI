package com.tongtongstudio.ami.data.datatables

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize
import java.text.DateFormat

@Parcelize
data class ProjectWithSubTasks(
    @Embedded
    val project: Project,
    @Relation(parentColumn = "p_id", entityColumn = "project_id", entity = Task::class)
    val subTasks: List<Task>
) : ThingToDo(project.pjtName, project.pjtPriority, project.pjtDeadline) {

    override fun getStartDate(): Long? {
        return project.pjtStartDate
    }

    fun getSubTasksNumber(): Int {
        return subTasks.size
    }

    override fun getCreatedDateFormatted(): String {
        // TODO: 24/01/2023 get correct format date formatted
        return DateFormat.getDateInstance().format(project.pjtCreatedDate)
    }

    override fun getEstimatedTime(): Long? {
        return project.pjtEstimatedTime
    }

    override fun getWorkTime(): Long? {
        return project.pjtWorkTime
    }

    override fun isCompleted(): Boolean {
        return project.isPjtCompleted
    }

    override fun getReminderDate(): Long? {
        return project.pjtReminder
    }
}