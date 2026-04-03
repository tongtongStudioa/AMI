package com.tongtongstudio.ami.domain.usecase

import com.google.gson.Gson
import com.tongtongstudio.ami.data.dao.AssessmentDao
import com.tongtongstudio.ami.data.dao.CategoryDao
import com.tongtongstudio.ami.data.dao.ReminderDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.dao.WorkSessionDao
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

class ExportDatabaseUseCase @Inject constructor(
    private val taskDao: TaskDao,
    private val workSessionDao: WorkSessionDao,
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val assessmentDao: AssessmentDao
) {

    suspend operator fun invoke(): String {

        val tasks = taskDao.getAllTasks().first()
        val workSessions = workSessionDao.getAllWorkSessions().first()
        val reminders = reminderDao.getAllReminders().first()
        val categories = categoryDao.getAllCategories().first()
        val assessments = assessmentDao.getAllAssessments().first()

        val dto = DatabaseBackupDto(
            tasks = tasks,
            workSessions = workSessions,
            reminders = reminders,
            categories = categories,
            assessments = assessments
        )

        return Gson().toJson(dto)
    }
}