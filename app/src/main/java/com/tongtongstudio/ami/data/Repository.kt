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

    fun getProjects(): Flow<List<TaskWithSubTasks>> {
        return ttdDao.getProjects()
    }

    fun getPotentialProjects(): Flow<List<Ttd>> {
        return ttdDao.getPotentialProject()
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

    fun getHabits(): Flow<List<TaskWithSubTasks>> {
        return ttdDao.getRecurringTasks()
    }

    // TODO: suppress this method and update composed task
    suspend fun updateProject(project: Project) {
        projectDao.update(project)
    }

    fun getUpcomingTasksCount(endDate: Long, endDateFilter: Long? = null): Flow<Int> {
        return if (endDateFilter != null)
            ttdDao.getUpcomingTasksCountFilter(endDate, endDateFilter)
        else ttdDao.getUpcomingTasksCount(endDate)
    }

    suspend fun getTasksAchievementRate(categoryId: Long? = null): Float {
        return if (categoryId != null) ttdDao.getCategoryTasksAchievementRate(categoryId) else ttdDao.getTotalAchievementRate()
    }

    suspend fun getProjectsAchievementRate(categoryId: Long? = null): Float {
        return if (categoryId != null) ttdDao.getCategoryProjectsAchievementRate(categoryId) else ttdDao.getAllProjectsAchievementRate()
    }

    private suspend fun getCompletedProjectsCount(categoryId: Long? = null): Int {
        return if (categoryId != null)
            ttdDao.getCategoryCompletedProjectsCount(categoryId)
        else ttdDao.getCompletedProjectsCount()
    }

    private suspend fun getCompletedProjectsCountByPeriod(
        categoryId: Long? = null,
        startDate: Long,
        endDate: Long
    ): Int {
        return if (categoryId != null)
            ttdDao.getCompletedCategoryTasksByPeriod(categoryId, startDate, endDate)
        else ttdDao.getCompletedTasksByPeriod(startDate, endDate)
    }

    suspend fun getMCompletedProjectsCount(
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Int {
        return if (startDate != null && endDate != null)
            getCompletedProjectsCountByPeriod(categoryId, startDate, endDate)
        else getCompletedProjectsCount(categoryId)
    }


    private suspend fun getCompletedTasksCount(categoryId: Long? = null): Int {
        return if (categoryId != null) ttdDao.getCategoryCompletedTasksCount(categoryId) else ttdDao.getCompletedTasksCount()
    }

    private suspend fun getCompletedTasksCountByPeriod(
        categoryId: Long? = null,
        startDate: Long,
        endDate: Long
    ): Int {
        return if (categoryId != null) ttdDao.getCompletedCategoryTasksByPeriod(
            categoryId,
            startDate,
            endDate
        ) else ttdDao.getCompletedTasksByPeriod(startDate, endDate)
    }

    suspend fun getMCompletedTasksCount(
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Int {
        return if (startDate != null && endDate != null)
            getCompletedTasksCountByPeriod(categoryId, startDate, endDate)
        else getCompletedTasksCount(categoryId)
    }

    suspend fun getAccuracyRateEstimation(
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        errorPercent: Float = 0.2F
    ): Float {
        return if (categoryId != null && startDate != null && endDate != null)
            ttdDao.getCategoryAccuracyRateOfEstimatedWorkTimeByPeriod(
                categoryId,
                startDate,
                endDate,
                errorPercent
            )
        else if (startDate != null && endDate != null)
            ttdDao.getAccuracyRateOfEstimatedWorkTimeByPeriod(startDate, endDate, errorPercent)
        else if (categoryId != null)
            ttdDao.getCategoryAccuracyRateOfEstimatedWorkTime(categoryId, errorPercent)
        else ttdDao.getAccuracyRateOfEstimatedWorkTime(errorPercent)
    }

    suspend fun getOnTimeCompletionRate(
        categoryId: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Float {
        return if (categoryId != null && startDate != null && endDate != null)
            ttdDao.getOnTimeCompletionTasksRateByCategoryAndPeriod(categoryId, startDate, endDate)
        else if (startDate != null && endDate != null)
            ttdDao.getOnTimeCompletionTasksRateByPeriod(startDate, endDate)
        else if (categoryId != null)
            ttdDao.getOnTimeCompletionCategoryTasksRate(categoryId)
        else ttdDao.getOnTimeCompletionTasksRate()
    }

    suspend fun getTimeWorked(categoryId: Long? = null): Long {
        return if (categoryId != null)
            ttdDao.getSumCategoryTimeWorked(categoryId)
        else ttdDao.getTotalTimeWorked()
    }

    suspend fun getMaxStreak(current: Boolean = false): Ttd {
        return if (current)
            ttdDao.getMaxCurrentStreakTask()
        else ttdDao.getMaxStreakTask()
    }

    suspend fun getHabitCompletionRate(categoryId: Long? = null): Float {
        return if (categoryId != null)
            ttdDao.getCategoryHabitCompletionRate(categoryId)
        else ttdDao.getHabitCompletionRate()
    }

}
