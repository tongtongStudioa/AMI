package com.tongtongstudio.ami.data

import com.tongtongstudio.ami.data.dao.*
import com.tongtongstudio.ami.data.datatables.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    // TODO: 04/02/2023 add comparative sorting for same name, deadline, etc, case
    fun getAllThingToDoToday(
        sortOrder: SortOrder,
        hideCompleted: Boolean,
        startOfToday: Long,
        endOfToday: Long,
    ): Flow<List<ThingToDo>> {

        val tasksFlow: Flow<List<ThingToDo>> =
            taskDao.getTodayTasks(hideCompleted, startOfToday, endOfToday)
        val eventsFlow: Flow<List<ThingToDo>> =
            eventDao.getTodayEvents(hideCompleted, startOfToday, endOfToday)
        val projectsWithSubtasksFlow: Flow<List<ThingToDo>> =
            projectDao.getTodayProjects(hideCompleted, startOfToday, endOfToday)

        //val thingsToDo: List<ThingToDo> = projectsWithSubtasks + events + tasks
        return combine(tasksFlow, eventsFlow, projectsWithSubtasksFlow) { tasks, events, projects ->
            tasks + events + projects
        }.map { allThingsToDo ->
            when (sortOrder) {
                SortOrder.BY_CREATOR_SORT ->
                    allThingsToDo.sortedWith(compareBy<ThingToDo> { it.isCompleted() }.thenBy { it.priority }
                        .thenBy { it.getEstimatedTime() })
                SortOrder.BY_NAME -> allThingsToDo.sortedBy { it.name }
                //SortOrder.BY_DEADLINE -> allThingsToDo.sortedByDescending { it.deadline }
                else -> allThingsToDo.sortedWith(compareBy<ThingToDo> { it.priority }.thenBy { it.name })
            }
        }
    }

    fun getAllLaterThingsToDo(endOfToday: Long, enOfDayFilter: Long?): Flow<List<ThingToDo>> {
        val tasksFlow: Flow<List<ThingToDo>> =
            if (enOfDayFilter != null) taskDao.getLaterTasksFilter(
                endOfToday,
                enOfDayFilter
            ) else taskDao.getLaterTasks(endOfToday)
        val eventsFlow: Flow<List<ThingToDo>> =
            if (enOfDayFilter != null) eventDao.getLaterEventsFilter(
                endOfToday,
                enOfDayFilter
            ) else eventDao.getLaterEvents(endOfToday)
        val projectsWithSubTasksFlow: Flow<List<ThingToDo>> =
            if (enOfDayFilter != null) projectDao.getLaterProjectsFilter(
                endOfToday,
                enOfDayFilter
            ) else projectDao.getLaterProjects(endOfToday)

        //val thingsToDo: List<ThingToDo> = projectsWithSubtasks + events + tasks
        return combine(
            tasksFlow,
            eventsFlow,
            projectsWithSubTasksFlow
        ) { tasks, events, projectsWithSubTasks ->
            tasks + events + projectsWithSubTasks
        }.map { allThingsToDo ->
            allThingsToDo.sortedWith(compareBy<ThingToDo> { it.getStartDate() }.thenBy { it.priority }
                .thenBy { it.getEstimatedTime() })
        }
    }

    fun getAllThingsToDoCompleted(): Flow<List<ThingToDo>> {
        val tasksFlow: Flow<List<ThingToDo>> = taskDao.getAllCompletedTasks()
        val projectsWithSubtasksFlow: Flow<List<ThingToDo>> = projectDao.getAllCompletedProjects()

        return combine(tasksFlow, projectsWithSubtasksFlow) { tasks, projects ->
            tasks + projects
        }.map { allThingsToDo ->
            allThingsToDo.sortedByDescending { it.getCompletedDate() }
        }
    }

    suspend fun getCountUpcomingTasks(endOfToday: Long): Int {
        return taskDao.getUpcomingTasksCount(endOfToday) + eventDao.getUpcomingEventsCount(
            endOfToday
        ) + projectDao.getUpcomingProjectsCount(endOfToday)
    }

    // task

    suspend fun insertAllSubTask(listSubTasks: List<Task>) {
        taskDao.insertUndoDeletedSubTasks(listSubTasks)
    }

    suspend fun deleteSubTasks(subTasks: List<Task>) {
        taskDao.deleteSubTasks(subTasks)
    }

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

    suspend fun getProjectData(projectId: Long): ProjectWithSubTasks {
        return projectDao.getProject(projectId)
    }

    fun getProjectCompletedStats(): Flow<List<Project>> {
        return projectDao.getProjectsStats()
    }

    suspend fun insertProject(project: Project): Long {
        return projectDao.insert(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.update(project)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.delete(project)
    }

    //event
    fun getAllEvents(hideCompleted: Boolean, sortOrder: SortOrder): Flow<List<Event>> {
        return eventDao.getAllEvents(hideCompleted, sortOrder)
    }

    suspend fun insertEvent(event: Event) {
        eventDao.insert(event)
    }

    suspend fun updateEvent(event: Event) {
        eventDao.update(event)
    }

    suspend fun deleteEvent(event: Event) {
        eventDao.delete(event)
    }
}
