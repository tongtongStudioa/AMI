package com.tongtongstudio.ami.domain.usecase

import com.google.gson.Gson
import com.tongtongstudio.ami.data.dao.AssessmentDao
import com.tongtongstudio.ami.data.dao.CategoryDao
import com.tongtongstudio.ami.data.dao.ReminderDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.dao.WorkSessionDao
import jakarta.inject.Inject

class ImportDatabaseUseCase @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao,
    private val workSessionDao: WorkSessionDao,
    private val assessmentDao: AssessmentDao
) {

    suspend operator fun invoke(json: String): Result<Unit> {
        return runCatching {

            val dto = Gson().fromJson(json, DatabaseBackupDto::class.java)
                ?: throw ImportException.InvalidFormat

            //clearDatabase()

            // Mapping
            val categories = dto.categories
            val tasks = dto.tasks
            val reminders = dto.reminders
            val workSessions = dto.workSessions
            val assessments = dto.assessments

            // Insert data (order to respect foreign constraints
            categoryDao.insertCategories(categories)
            taskDao.insertTasks(tasks)
            workSessionDao.insertWorkSessions(workSessions)
            reminderDao.insertReminders(reminders)
            assessmentDao.insertAssessments(assessments)
        }
    }

    private suspend fun clearDatabase() {
        /*taskDao.deleteAll()
        categoryDao.deleteAll()
        reminderDao.deleteAll()
        workSessionDao.deleteAll()
        assessmentDao.deleteAll()*/
    }
}