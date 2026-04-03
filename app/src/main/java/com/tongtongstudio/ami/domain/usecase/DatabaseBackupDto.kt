package com.tongtongstudio.ami.domain.usecase

import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.WorkSession

data class DatabaseBackupDto(
    val tasks: List<Task> = emptyList(),
    val workSessions: List<WorkSession> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val categories: List<Category> = emptyList(),
    val assessments: List<Assessment> = emptyList()
)