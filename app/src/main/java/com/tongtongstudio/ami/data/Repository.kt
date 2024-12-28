package com.tongtongstudio.ami.data

import com.tongtongstudio.ami.data.dao.AssessmentDao
import com.tongtongstudio.ami.data.dao.CategoryDao
import com.tongtongstudio.ami.data.dao.ReminderDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.dao.WorkSessionDao
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.TimeWorkedDistribution
import com.tongtongstudio.ami.data.datatables.TtdAchieved
import com.tongtongstudio.ami.data.datatables.TtdStreakInfo
import com.tongtongstudio.ami.data.datatables.WorkSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class Repository @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao,
    private val assessmentDao: AssessmentDao,
    private val workSessionDao: WorkSessionDao
) {

    fun getThingsToDoToday(
        sortOrder: SortOrder,
        hideCompleted: Boolean,
        hideLateTasks: Boolean,
        startOfToday: Long,
        endOfToday: Long,
    ): Flow<List<ThingToDo>> {
        return taskDao.getTodayTasks(
            sortOrder,
            hideCompleted,
            hideLateTasks,
            startOfToday,
            endOfToday
        )
    }

    fun getLaterThingsToDo(
        endDayDate: Long,
        endDayFilter: Long?,
        sortOrder: SortOrder? = null,
    ): Flow<List<ThingToDo>> {
        return if (endDayFilter != null) {
            taskDao.getLaterTasksFilter(endDayDate, endDayFilter)
        } else taskDao.getLaterTasks(endDayDate)
    }

    fun getCompletedTasks(): Flow<List<ThingToDo>> {
        return taskDao.getCompletedTasks()
    }

    suspend fun getTask(id: Long): Task {
        return taskDao.getTask(id)
    }

    suspend fun insertTask(task: Task): Long {
        return taskDao.insert(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
    }

    fun getProjects(hideCompleted: Boolean): Flow<List<ThingToDo>> {
        return taskDao.getProjects(hideCompleted)
    }

    fun getPotentialProjects(): Flow<List<Task>> {
        return taskDao.getPotentialProject()
    }

    suspend fun getMissedRecurringTasks(todayDate: Long): List<ThingToDo> {
        return taskDao.getMissedRecurringTasks(todayDate)
    }

    suspend fun getCategoryById(id: Long): Category {
        return categoryDao.getById(id)
    }

    suspend fun getCategoryByTitle(title: String): Category? {
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

    fun getGoalAssessments(taskId: Long?): Flow<MutableList<Assessment>>? {
        return if (taskId != null)
            assessmentDao.getIntermediateAssessments(taskId)
        else null
    }

    suspend fun insertAssessment(newAssessment: Assessment): Long {
        return assessmentDao.insert(newAssessment)
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

    fun getHabits(): Flow<List<ThingToDo>> {
        return taskDao.getRecurringTasks()
    }

    fun getUpcomingTasksCount(endDate: Long, endDateFilter: Long? = null): Flow<Int> {
        return if (endDateFilter != null)
            taskDao.getUpcomingTasksCountFilter(endDate, endDateFilter)
        else taskDao.getUpcomingTasksCount(endDate)
    }

    fun getTasksAchievementRate(categoryId: Long? = null): Flow<Float> {
        return if (categoryId != null) taskDao.getCategoryTasksAchievementRate(categoryId) else taskDao.getTotalAchievementRate()
    }

    fun getProjectsAchievementRate(categoryId: Long? = null): Flow<Float> {
        return if (categoryId != null) taskDao.getCategoryProjectsAchievementRate(categoryId) else taskDao.getAllProjectsAchievementRate()
    }

    fun getCompletedProjectsCount(categoryId: Long? = null): Flow<Int> {
        return if (categoryId != null)
            taskDao.getCategoryCompletedProjectsCount(categoryId)
        else taskDao.getCompletedProjectsCount()
    }

    fun getCompletedProjectsCountByPeriod(
        categoryId: Long? = null,
        startDate: Long,
        endDate: Long
    ): Flow<List<TtdAchieved?>?> {
        return if (categoryId != null)
            taskDao.getCompletedCategoryTasksByPeriod(categoryId, startDate, endDate)
        else taskDao.getCompletedTasksByPeriod(startDate, endDate)
    }


    fun getCompletedTasksCount(categoryId: Long? = null): Flow<Int> {
        return if (categoryId != null) taskDao.getCategoryCompletedTasksCount(categoryId) else taskDao.getCompletedTasksCount()
    }

    fun getCompletedTasksCountByPeriod(
        categoryId: Long? = null,
        startDate: Long,
        endDate: Long
    ): Flow<List<TtdAchieved?>?> {
        return if (categoryId != null) taskDao.getCompletedCategoryTasksByPeriod(
            categoryId,
            startDate,
            endDate
        ) else taskDao.getCompletedTasksByPeriod(startDate, endDate)
    }

    fun getTimeWorkedGrouped(categoryId: Long? = null): Flow<List<TimeWorkedDistribution>> {
        return if (categoryId != null)
            getTimeWorkedByTask(categoryId)
        else getTimeWorkedByCategory()
    }

    private fun getTimeWorkedByCategory(): Flow<List<TimeWorkedDistribution>> {
        return taskDao.getTimeWorkedPerCategory()
    }

    private fun getTimeWorkedByTask(categoryId: Long): Flow<List<TimeWorkedDistribution>> {
        return taskDao.getRateTimeWorkedPerTask(categoryId)
    }

    fun getAccuracyRateEstimation(
        categoryId: Long? = null,
        errorPercent: Float = 0.3F
    ): Flow<Float?> {
        return if (categoryId != null)
            taskDao.getCategoryAccuracyRateOfEstimatedWorkTime(categoryId, errorPercent)
        else taskDao.getAccuracyRateOfEstimatedWorkTime(errorPercent)
    }

    fun getOnTimeCompletionRate(
        categoryId: Long? = null,
    ): Flow<Float?> {
        return if (categoryId != null)
            taskDao.getOnTimeCompletionCategoryTasksRate(categoryId)
        else taskDao.getOnTimeCompletionTasksRate()
    }

    fun getTimeWorked(categoryId: Long? = null): Flow<Long> {
        return if (categoryId != null)
            taskDao.getSumCategoryTimeWorked(categoryId)
        else taskDao.getTotalTimeWorked()
    }

    fun getMaxStreak(categoryId: Long? = null): Flow<TtdStreakInfo> {
        return if (categoryId != null)
            taskDao.getMaxStreakCategoryTask(categoryId)
        else taskDao.getMaxStreakTask()
    }

    fun getCurrentMaxStreak(categoryId: Long? = null): Flow<TtdStreakInfo> {
        return if (categoryId != null)
            taskDao.getCurrentMaxStreakCategoryTask(categoryId)
        else taskDao.getCurrentMaxStreakTask()
    }

    fun getHabitCompletionRate(categoryId: Long? = null): Flow<Float?> {
        return if (categoryId != null)
            taskDao.getCategoryHabitCompletionRate(categoryId)
        else taskDao.getHabitCompletionRate()
    }

    suspend fun getComposedTask(parentTaskId: Long): ThingToDo {
        return taskDao.getComposedTask(parentTaskId)
    }

    fun getGlobalGoals(): Flow<List<Assessment>> {
        return assessmentDao.getGlobalGoals()
    }

    fun getDraftsTasks(): Flow<List<ThingToDo>> {
        return taskDao.getDraftTask()
    }

    fun getWorkSessions(taskId: Long): Flow<List<WorkSession>> {
        return workSessionDao.getWorkSessions(taskId)
    }

    suspend fun suppressWorkSession(workSession: WorkSession) {
        workSessionDao.delete(workSession)
    }

    suspend fun insertWorkSession(workSession: WorkSession) {
        workSessionDao.insert(workSession)
    }

    fun getTaskTimeWorked(taskId: Long?): Flow<Long> {
        return if (taskId != null)
            workSessionDao.getTaskTimeWorked(taskId)
        else flowOf(0)
    }

    suspend fun updateTasksUrgency(todayDate: Long) {
        TODO("Not yet implemented")
        val taskList = taskDao.getTasksNotCompeted().first()
        for (task in taskList) {
            val urgency = Task.calculusUrgency(todayDate, task.dueDate!!, task.deadline)
            val priority = Task.calculatingPriority(task.priority, task.importance, task.urgency)
            taskDao.update(task.copy(urgency = urgency, priority = priority))
        }
    }

    fun getTaskMaxStreak(id: Long): Int {
        TODO("Not yet implemented")
    }

}
