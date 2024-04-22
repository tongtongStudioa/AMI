package com.tongtongstudio.ami.data

import com.tongtongstudio.ami.data.dao.*
import com.tongtongstudio.ami.data.datatables.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class Repository @Inject constructor(
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val eventDao: EventDao,
    private val ttdDao: TtdDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao,
    private val assessmentDao: AssessmentDao
) {

    fun getThingsToDoToday(
        sortOrder: SortOrder,
        hideCompleted: Boolean,
        startOfToday: Long,
        endOfToday: Long,
    ): Flow<List<TaskWithSubTasks>> {
        // TODO: Change sorOrder class (add ordering rules)
        return ttdDao.getTodayTasks(sortOrder, hideCompleted, startOfToday, endOfToday)
    }

    fun getLaterThingsToDo(
        endDayDate: Long,
        endDayFilter: Long?,
        sortOrder: SortOrder? = null,
    ): Flow<List<TaskWithSubTasks>> {
        return if (endDayFilter != null) {
            ttdDao.getLaterTasksFilter(endDayDate, endDayFilter)
        } else ttdDao.getLaterTasks(endDayDate)
    }

    fun getCompletedTasks(): Flow<List<TaskWithSubTasks>> {
        return ttdDao.getCompletedTasks()
    }

    suspend fun getTask(id: Long): Ttd {
        return ttdDao.getTask(id)
    }

    suspend fun insertTask(task: Ttd): Long {
        return ttdDao.insert(task)
    }

    suspend fun updateTask(task: Ttd) {
        ttdDao.update(task)
    }

    suspend fun deleteTask(task: Ttd) {
        ttdDao.delete(task)
    }

    fun getProjects(): Flow<List<Ttd>> {
        return ttdDao.getTaskComposed()
    }

    suspend fun getMissedRecurringTasks(todayDate: Long): List<Ttd> {
        return ttdDao.getMissedRecurringTasks(todayDate)
    }

    suspend fun getCategoryById(id: Long): Category {
        return categoryDao.getById(id)
    }

    suspend fun getCategoryByTitle(title: String): Category {
        return categoryDao.getByTitle(title)
    }

    fun getCategories(): Flow<List<Category>> {
        return categoryDao.getCategories()
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun getAssessment(id: Long): Assessment {
        return assessmentDao.get(id)
    }

    fun getTasksAssessments(taskId: Long?): Flow<MutableList<Assessment>>? {
        return if (taskId != null)
            assessmentDao.getTaskAssessments(taskId)
        else null
    }

    suspend fun insertAssessment(newAssessment: Assessment) {
        assessmentDao.insert(newAssessment)
    }

    suspend fun updateAssessment(assessment: Assessment) {
        assessmentDao.update(assessment)
    }

    suspend fun deleteAssessment(assessment: Assessment) {
        assessmentDao.delete(assessment)
    }

    fun getTaskReminders(id: Long?): Flow<MutableList<Reminder>>? {
        return if (id != null)
            reminderDao.getTaskReminders(id)
        else null
    }

    suspend fun insertReminder(reminder: Reminder) {
        reminderDao.insert(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.delete(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.update(reminder)
    }

    fun getHabits(): Flow<List<Ttd>> {
        return ttdDao.getRecurringTasks()
    }

    suspend fun getCountUpcomingTasks(endOfToday: Long): Int {
        return taskDao.getUpcomingTasksCount(endOfToday) + eventDao.getUpcomingEventsCount(
            endOfToday
        ) + projectDao.getUpcomingProjectsCount(endOfToday)
    }

    // task
    fun getTasksCompletedStats(): Flow<List<Task>> {
        return taskDao.getTasksCompletedStats()
    }

    //project
    fun getAllProjects(
        sortOrder: SortOrder,
        hideCompleted: Boolean
    ): Flow<List<ProjectWithSubTasks>> {
        return projectDao.getProjectsWithTasks(sortOrder, hideCompleted)
    }

    fun getProjectCompletedStats(): Flow<List<Project>> {
        return projectDao.getProjectsStats()
    }

    suspend fun updateProject(project: Project) {
        projectDao.update(project)
    }
}
