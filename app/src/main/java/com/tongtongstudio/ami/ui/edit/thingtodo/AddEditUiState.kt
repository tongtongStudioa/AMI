package com.tongtongstudio.ami.ui.edit.thingtodo

import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.Type

data class AddEditUiState(
    // Données de base
    val thingToDo: ThingToDo? = null,
    val title: String = "",
    val description: String? = null,
    val creationDateFormatted: String? = null,
    val type: String? = Type.UNIQUE.name,

    // Dates
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val deadline: Long? = null,

    // Priority and importance
    val priority: Int? = null,
    val importance: Int? = null,
    val urgency: Int? = null,

    // Relations
    val parentProject: Task? = null,
    val blockingTask: Task? = null, //predecessorTask
    val category: Category? = null,

    // Metadata
    val nature: String? = Nature.TASK.name,
    val estimatedWorkTime: Long? = null,
    val estimatedEmotions: Int = 1,
    val skillLevel: Int? = null,
    val taskRecurrenceWithDays: TaskRecurrenceWithDays? = null,

    // Reminders
    val reminders: List<Reminder> = emptyList(),
    // Categories
    val categorySuggestions: List<Category> = emptyList(),

    // Ui state
    val isLoading: Boolean = false,
    val error: String? = null
)