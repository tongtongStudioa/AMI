package com.tongtongstudio.ami.data

import com.tongtongstudio.ami.data.dao.AssessmentDao
import com.tongtongstudio.ami.data.dao.CategoryDao
import com.tongtongstudio.ami.data.dao.RecurrenceInfoDao
import com.tongtongstudio.ami.data.dao.ReminderDao
import com.tongtongstudio.ami.data.dao.TaskCompletionDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.dao.WorkSessionDao
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.DaysOfWeek
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.ReminderNotification
import com.tongtongstudio.ami.data.datatables.Status
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskCompletion
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceDaysCrossRef
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.ThingToDoDetails
import com.tongtongstudio.ami.data.datatables.TimeWorkedDistribution
import com.tongtongstudio.ami.data.datatables.TtdAchieved
import com.tongtongstudio.ami.data.datatables.TtdStreakInfo
import com.tongtongstudio.ami.data.datatables.Type
import com.tongtongstudio.ami.data.datatables.WorkSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar
import javax.inject.Inject

class Repository @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao,
    private val assessmentDao: AssessmentDao,
    private val workSessionDao: WorkSessionDao,
    private val recurrenceInfoDao: RecurrenceInfoDao,
    private val taskCompletionDao: TaskCompletionDao,
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
            taskDao.getLaterTasks(endDayDate, endDayFilter)
        } else taskDao.getLaterTasks(endDayDate)
    }

    fun getCompletedTasks(): Flow<List<ThingToDo>> {
        return taskDao.getCompletedTasks()
    }

    suspend fun getTask(id: Long): Task {
        return taskDao.getTask(id)
    }

    suspend fun insertTask(task: Task): Long {
        val taskId = taskDao.insert(task)
        task.parentTaskId?.let {
            updateProject(it)
        }
        return taskId
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task)
        // Mettre à jour la progression du parent
        task.parentTaskId?.let { parentId ->
            updateProject(parentId)
        }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
        task.parentTaskId?.let {
            updateProject(it)
        }
    }

    fun getProjects(hideCompleted: Boolean): Flow<List<ThingToDo>> {
        return taskDao.getProjects(hideCompleted)
    }

    fun getPotentialLinkedTasks(taskIdRemoved: Long?): Flow<List<Task>> {
        return if (taskIdRemoved != null)
            taskDao.getPotentialParentTasks(taskIdRemoved)
        else taskDao.getPotentialParentTasks()
    }

    suspend fun getMissedRecurringTasks(todayDate: Long): List<ThingToDo> {
        return taskDao.getMissedRecurringTasks(todayDate)
    }

    fun getDraftsTasks(): Flow<List<ThingToDo>> {
        return taskDao.getDraftTask()
    }

    suspend fun updateTasksUrgency(todayDate: Long) {
        TODO("Not yet implemented")
        val taskList = taskDao.getTasksNotCompleted().first()
        for (task in taskList) {
            if (task.dueDate == null)
                return
            val urgency = Task.calculusUrgency(task.dueDate, task.deadline, todayDate)
            val priority = Task.calculatingPriority(task.priority, task.importance, task.urgency)
            taskDao.update(task.copy(urgency = urgency, priority = priority))
        }
    }

    fun getSubTasks(parentTaskId: Long): Flow<List<Task>> {
        return taskDao.getSubTasks(parentTaskId)
    }

    // ****** Categories ******* //
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

    /** ****** Assessment and Goals ******* **/
    suspend fun getAssessment(id: Long): Assessment {
        return assessmentDao.get(id)
    }

    fun getIntermediateAssessmentsByGoal(parentId: Long?): Flow<MutableList<Assessment>>? {
        return if (parentId != null)
            assessmentDao.getIntermediateAssessments(parentId)
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

    fun getGlobalGoals(): Flow<List<Assessment>> {
        return assessmentDao.getGlobalGoals()
    }

    /** ******* Reminders ****** **/
    fun getTaskReminders(id: Long?): Flow<MutableList<Reminder>>? {
        return if (id != null)
            reminderDao.getTaskReminders(id)
        else null
    }

    suspend fun insertReminder(reminder: Reminder): Long {
        return reminderDao.insert(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.delete(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.update(reminder)
    }

    /** ****** Work Sessions ******* **/
    suspend fun suppressWorkSession(workSession: WorkSession) {
        workSessionDao.delete(workSession)
        updateTaskProgress(workSession.parentTaskId)
    }

    suspend fun insertWorkSession(workSession: WorkSession) {
        workSessionDao.insert(workSession)
        updateTaskProgress(workSession.parentTaskId)
    }

    fun getWorkSessions(taskId: Long): Flow<List<WorkSession>> {
        return workSessionDao.getWorkSessions(taskId)
    }

    suspend fun updateWorkSession(workSession: WorkSession) {
        workSessionDao.update(workSession)
    }

    /** ****** Stats ******* **/
    fun getHabits(): Flow<List<ThingToDo>> {
        return taskDao.getRecurringTasks()
    }

    fun getUpcomingTasksCount(endDate: Long, endDateFilter: Long? = null): Flow<Int> {
        return if (endDateFilter != null)
            taskDao.getUpcomingTasksCountFilter(endDate, endDateFilter)
        else taskDao.getUpcomingTasksCount(endDate)
    }

    suspend fun getTasksAchievementRate(categoryId: Long? = null): Float {
        return if (categoryId != null) taskDao.getAchievementRateByCategory(categoryId) else taskDao.getAchievementRate()
    }

    suspend fun getProjectsAchievementRate(categoryId: Long? = null): Float {
        return if (categoryId != null) taskDao.getProjectsAchievementRateByCategory(categoryId) else taskDao.getProjectsAchievementRate()
    }

    suspend fun getCompletedProjectsCount(categoryId: Long? = null): Int {
        return if (categoryId != null)
            taskDao.getCategoryCompletedProjectsCount(categoryId)
        else taskDao.getCompletedProjectsCount()
    }

    suspend fun getCompletedProjectsCountByPeriod(
        categoryId: Long? = null,
        startDate: Long,
        endDate: Long
    ): List<TtdAchieved> {
        return if (categoryId != null)
            taskDao.getCompletedTasksByPeriod(categoryId, startDate, endDate)
        else taskDao.getCompletedTasksByPeriod(startDate, endDate)
    }


    suspend fun getCompletedTasksCount(categoryId: Long? = null): Int {
        return if (categoryId != null) taskDao.getCompletedTasksCount(categoryId) else taskDao.getCompletedTasksCount()
    }

    suspend fun getCompletedTasksCountByPeriod(
        categoryId: Long? = null,
        startDate: Long,
        endDate: Long
    ): List<TtdAchieved?> {
        return if (categoryId != null) taskDao.getCompletedTasksByPeriod(
            categoryId,
            startDate,
            endDate
        ) else taskDao.getCompletedTasksByPeriod(startDate, endDate)
    }

    suspend fun getTimeWorkedGrouped(categoryId: Long? = null): List<TimeWorkedDistribution> {
        return if (categoryId != null)
            getTimeWorkedByTask(categoryId)
        else getTimeWorkedByCategory()
    }

    private suspend fun getTimeWorkedByCategory(): List<TimeWorkedDistribution> {
        return taskDao.getTimeWorkedPerCategory()
    }

    private suspend fun getTimeWorkedByTask(categoryId: Long): List<TimeWorkedDistribution> {
        return taskDao.getTimeWorkedPerTask(categoryId)
    }

    suspend fun getAccuracyRateEstimation(
        categoryId: Long? = null,
        errorPercent: Float = 0.3F
    ): Float {
        return if (categoryId != null)
            taskDao.getCategoryAccuracyRateOfEstimatedWorkTime(categoryId, errorPercent)
        else taskDao.getAccuracyRateOfEstimatedWorkTime(errorPercent)
    }

    suspend fun getOnTimeCompletionRate(
        categoryId: Long? = null,
    ): Float? {
        return if (categoryId != null)
            taskDao.getOnTimeCompletionCategoryTasksRate(categoryId)
        else taskDao.getOnTimeCompletionTasksRate()
    }

    suspend fun getTimeWorked(categoryId: Long? = null): Long {
        return if (categoryId != null)
            taskDao.getTotalTimeWorked(categoryId)
        else taskDao.getTotalTimeWorked()
    }

    suspend fun getMaxStreak(categoryId: Long? = null): TtdStreakInfo {
        return if (categoryId != null)
            taskDao.getTaskMaxStreak(categoryId)
        else taskDao.getTaskMaxStreak()
    }

    suspend fun getCurrentMaxStreak(categoryId: Long? = null): TtdStreakInfo {
        return if (categoryId != null)
            taskDao.getTaskCurrentMaxStreak(categoryId)
        else taskDao.getTaskCurrentMaxStreak()
    }

    suspend fun getHabitCompletionRate(categoryId: Long? = null): Float {
        return if (categoryId != null)
            taskDao.getCategoryHabitCompletionRate(categoryId)
        else taskDao.getHabitCompletionRate()
    }

    fun getTaskTimeWorked(taskId: Long?): Flow<Long> {
        return if (taskId != null)
            workSessionDao.getTaskTimeWorked(taskId)
        else flowOf(0)
    }

    // TODO: Create a recursive function which emit a flow to automatically change value
    suspend fun getThingToDoTimeWorked(taskId: Long?): Flow<Long> {
        if (taskId == null)
            return flowOf(0L)
        val taskWorkTime = workSessionDao.getTaskTimeWorked(taskId)
        val directSubTasks = taskDao.getSubTasks(taskId)
        var totalWorkTime = taskWorkTime
        directSubTasks.collect { subTask ->
            subTask.forEach {
                totalWorkTime =
                    flowOf(totalWorkTime.first() + getThingToDoTimeWorked(it.id).first())
            }
        }
        return totalWorkTime
    }

    suspend fun getProjectTimeWorked(mainTaskId: Long?): Long {
        if (mainTaskId == null)
            return 0L
        val directSubTasks = taskDao.getSubTasks(mainTaskId).first()
        var projectWorkTime = 0L
        directSubTasks.forEach {
            projectWorkTime += workSessionDao.getTaskTimeWorked(it.id).first()
            projectWorkTime += getProjectTimeWorked(it.id)
        }
        return projectWorkTime
    }

    suspend fun getTotalEstimatedWorkTime(parentId: Long): Long? {
        val directSubTasks = taskDao.getSubTasks(parentId).first()
        var totalEstimatedWorkTime = 0L
        directSubTasks.forEach {
            totalEstimatedWorkTime += it.estimatedWorkingTime ?: 0L
            totalEstimatedWorkTime += getTotalEstimatedWorkTime(it.id) ?: 0L
        }
        return if (totalEstimatedWorkTime == 0L) null else totalEstimatedWorkTime
    }

    suspend fun getBlockingTask(blockingTaskId: Long): Task {
        return taskDao.getTask(blockingTaskId)
    }

    suspend fun getTaskRecurrenceWithDays(recurrenceId: Long): TaskRecurrenceWithDays? {
        val taskRecurrence = recurrenceInfoDao.getTaskRecurrenceById(recurrenceId)
        return if (taskRecurrence != null) {
            val taskRecurrenceWithDays = recurrenceInfoDao.getTaskRecurrenceWithDays(recurrenceId)
            //Log.e("repository", taskRecurrenceWithDays.toString())
            taskRecurrenceWithDays
        } else null
    }

    suspend fun toggleTaskCompletion(
        type: String,
        task: Task,
        isChecked: Boolean,
        lastCompletionStatus: Boolean
    ) {
        if (type != Type.RECURRING.name) {
            val newCompletion = TaskCompletion(task.id, isChecked)
            taskCompletionDao.deleteLastTaskCompletion(task.id)
            taskCompletionDao.insert(newCompletion)
        } else {
            // Get old recurring task's due date
            val oldDueDate = task.dueDate!!
            // Get task recurrence informations
            val taskRecurrenceWithDays =
                recurrenceInfoDao.getTaskRecurrenceWithDays(task.recurrenceInfosId!!)
            //Log.e("Inside toggleCompletion", "task recurrence is set : $taskRecurrenceWithDays!")
            // Insert TaskCompletion not complete for each due date before present day
            val allDueDates = taskRecurrenceWithDays.calculateAllDueDatesBetween(
                oldDueDate,
                Calendar.getInstance().timeInMillis
            )
            allDueDates.forEach { dueDate ->
                if (isChecked && dueDate == oldDueDate) {
                    val newCompletion = TaskCompletion(task.id, true, completionDate = dueDate)
                    taskCompletionDao.insert(newCompletion)
                } else
                    taskCompletionDao.insert(
                        TaskCompletion(
                            task.id,
                            isCompleted = false,
                            completionDate = dueDate
                        )
                    )
            }
            if (isChecked) {
                val newCompletion = TaskCompletion(task.id, true, oldDueDate)
                taskCompletionDao.insert(newCompletion)
            }
            //Log.e("Inside toggleCompletion","all due dates inserted")
        }

        var updatedTask = task.copy(
            // TODO: update status for en of recurring task
            status = if (!lastCompletionStatus && isChecked && type != Type.RECURRING.name) Status.FINISHED.name
            else if (isChecked) Status.IN_PROGRESS.name else Status.REVIEW.name,
        )
        if (type == Type.RECURRING.name) {
            val taskRecurrenceWithDays =
                recurrenceInfoDao.getTaskRecurrenceWithDays(task.recurrenceInfosId!!)
            // Update task with new due date and complete task if new due date > end date or occurrence limit reached
            val newDueDate = taskRecurrenceWithDays.getNextOccurrenceDay(task.dueDate!!)
            updatedTask = updatedTask.copy(
                dueDate = newDueDate
            )
        }
        // trough repository update's method
        updateTask(updatedTask)
        //Log.e("Inside toggleCompletion","Task updated : $updatedTask")
    }

    private suspend fun updateProject(parentId: Long) {
        val project = taskDao.getTask(parentId)
        val subtasks = taskDao.getSubThingTodos(parentId).first()
        val totalSubtasks = subtasks.size
        val completedSubtasks = subtasks.count { it.lastCompletionStatus == true }
        val isCompleted = totalSubtasks == completedSubtasks

        val status = if (isCompleted)
            Status.FINISHED.name
        else if (completedSubtasks > 0)
            Status.IN_PROGRESS.name
        else project.status

        val newNature = when {
            totalSubtasks == 0 && project.parentTaskId != null -> Nature.SUB_TASK.name
            totalSubtasks == 0 -> Nature.TASK.name
            project.parentTaskId != null -> Nature.INTERMEDIATE_PROJECT.name
            else -> Nature.PROJECT.name
        }

        taskDao.update(project.copy(status = status, nature = newNature))
        val projectCompletion = TaskCompletion(parentId, isCompleted)
        if (isCompleted)
            taskCompletionDao.insert(projectCompletion)
        else taskCompletionDao.deleteLastTaskCompletion(parentId)

        // Propager la mise à jour vers le parent (récursivement)
        project.parentTaskId?.let { parentId ->
            updateProject(parentId)
        }
    }

    suspend fun updateTaskCompletion(completion: TaskCompletion) {
        taskCompletionDao.update(completion)
    }

    suspend fun deleteLastTaskCompletion(id: Long) {
        taskCompletionDao.deleteLastTaskCompletion(id)
    }

    suspend fun getThingToDoDetails(id: Long): ThingToDoDetails {
        return taskDao.getThingToDoDetails(id)
    }

    fun getSubThingToDo(parentId: Long): Flow<List<ThingToDo>> {
        return taskDao.getSubThingTodos(parentId)
    }

    fun getThingToDo(taskId: Long?): Flow<ThingToDo>? {
        if (taskId == null)
            return null
        return taskDao.getThingToDo(taskId)
    }

    fun getLastTaskCompletion(parentTaskId: Long): Flow<TaskCompletion?> {
        return taskCompletionDao.getLastTaskCompletion(parentTaskId)
    }

    suspend fun insertOrUpdateTaskRecurrenceWithDays(taskRecurrenceWithDays: TaskRecurrenceWithDays): Long {
        val taskRecurrence = taskRecurrenceWithDays.taskRecurrence
        val alreadyExist =
            recurrenceInfoDao.getTaskRecurrenceById(taskRecurrence.recurrenceId) != null
        return if (alreadyExist) {
            recurrenceInfoDao.updateRecurringInfo(taskRecurrence)

            val crossRefForDays = createCrossRefForDays(taskRecurrenceWithDays)
            recurrenceInfoDao.insertCrossRefForDays(crossRefForDays)
            taskRecurrence.recurrenceId
        } else {
            val newId = recurrenceInfoDao.insertRecurringInfo(taskRecurrence)
            val crossRefForDays = createCrossRefForDays(
                taskRecurrenceWithDays.copy(
                    taskRecurrenceWithDays.taskRecurrence.copy(recurrenceId = newId)
                )
            )
            recurrenceInfoDao.insertCrossRefForDays(crossRefForDays)
            newId
        }
    }

    private suspend fun createCrossRefForDays(taskRecurrenceWithDays: TaskRecurrenceWithDays): List<TaskRecurrenceDaysCrossRef> {
        val recurrenceId = taskRecurrenceWithDays.taskRecurrence.recurrenceId
        return buildList {
            for (day in taskRecurrenceWithDays.daysOfWeek) {
                val crossRef = recurrenceInfoDao.getCrossRefDay(recurrenceId, day.dayId.toLong())
                if (crossRef != null)
                    add(crossRef.copy(recurrenceId, day.dayId.toLong()))
                else
                    add(TaskRecurrenceDaysCrossRef(recurrenceId, day.dayId.toLong()))
            }
        }
    }

    suspend fun updateTaskRecurrence(taskRecurrence: TaskRecurrence): Long {
        recurrenceInfoDao.updateRecurringInfo(taskRecurrence)
        return taskRecurrence.recurrenceId
    }

    suspend fun getDaysOfWeek(daysId: List<Int>): List<DaysOfWeek> {
        return recurrenceInfoDao.getDaysOfWeeks(daysId)
    }

    suspend fun getHabitCompletionRate(taskId: Long): Float {
        return taskDao.getHabitCompletionRate(taskId)
    }

    suspend fun getMaxStreak(taskId: Long): Int {
        return taskDao.getLongestStreak(taskId)
    }

    suspend fun getHabitCompletionCount(taskId: Long): Int {
        return taskDao.getHabitCompletionCount(taskId)
    }

    suspend fun getCurrentStreak(taskId: Long): Int {
        return taskDao.getCurrentStreakByTask(taskId)
    }

    suspend fun updateSubTasksCategory(parentTaskId: Long, categoryId: Long?) {
        val directSubTasks = taskDao.getSubTasks(parentTaskId).first()
        if (directSubTasks.isEmpty())
            return
        directSubTasks.forEach {
            taskDao.update(it.copy(categoryId = categoryId))
            updateSubTasksCategory(it.id, categoryId)
        }
    }

    suspend fun updateTaskProgress(taskId: Long) {
        val task = taskDao.getTask(taskId)
        val workSessionCount = workSessionDao.getWorkSessionsCountByTaskId(taskId)
        updateTask(task.copy(status = if (workSessionCount > 0) Status.IN_PROGRESS.name else Status.NOT_STARTED.name))
    }

    suspend fun getReminderNotification(reminderId: Long): ReminderNotification? {
        return reminderDao.getReminderNotification(reminderId)
    }
    // ************* //
}
