package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.Project
import com.tongtongstudio.ami.data.datatables.ProjectWithSubTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    // samples methode
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    // to get all project to finish today
    @Transaction
    @Query("SELECT * FROM project_table WHERE (is_pjt_completed != :hideCompleted OR is_pjt_completed == 0) AND (pjtStartDate BETWEEN :startOfToday AND :endOfToday OR pjtStartDate <= :endOfToday AND is_pjt_completed == 0 OR pjtDeadline <= :endOfToday AND is_pjt_completed == 0)")
    fun getTodayProjects(
        hideCompleted: Boolean,
        startOfToday: Long,
        endOfToday: Long
    ): Flow<List<ProjectWithSubTasks>>

    // to get all projects to begin or finish later
    //without filter
    @Transaction
    @Query("SELECT * FROM project_table WHERE is_pjt_completed == 0 AND (pjtStartDate > :endOfToday OR pjtDeadline > :endOfToday) ")
    fun getLaterProjects(endOfToday: Long): Flow<List<ProjectWithSubTasks>>

    // with filter
    @Transaction
    @Query("SELECT * FROM project_table WHERE is_pjt_completed == 0 AND (pjtStartDate BETWEEN :endOfToday AND :endOfDayFilter OR pjtDeadline BETWEEN :endOfToday AND :endOfDayFilter)")
    fun getLaterProjectsFilter(
        endOfToday: Long,
        endOfDayFilter: Long
    ): Flow<List<ProjectWithSubTasks>>

    // to get all projects with filter
    fun getProjectsWithTasks(
        sortOrder: SortOrder,
        hideCompleted: Boolean
    ): Flow<List<ProjectWithSubTasks>> =
        when (sortOrder) {
            SortOrder.BY_CREATOR_SORT -> getAllProjectsByPriority(hideCompleted)
            SortOrder.BY_DEADLINE -> getAllProjectsByDeadline(
                hideCompleted
            )
            SortOrder.BY_NAME -> getAllProjectsByName(hideCompleted)
            else -> getAllProjectsByPriority(hideCompleted)
        }

    @Transaction
    @Query("SELECT * FROM project_table WHERE (is_pjt_completed != :hideCompleted OR is_pjt_completed = 0) ORDER BY pjtName ")
    fun getAllProjectsByName(hideCompleted: Boolean): Flow<List<ProjectWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM project_table WHERE (is_pjt_completed != :hideCompleted OR is_pjt_completed = 0) ORDER BY is_pjt_completed, pjtPriority DESC")
    fun getAllProjectsByPriority(hideCompleted: Boolean): Flow<List<ProjectWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM project_table WHERE (is_pjt_completed != :hideCompleted OR is_pjt_completed = 0) ORDER BY pjtDeadline DESC")
    fun getAllProjectsByDeadline(hideCompleted: Boolean): Flow<List<ProjectWithSubTasks>>

    // to get all projects completed
    @Transaction
    @Query("SELECT * FROM project_table WHERE is_pjt_completed ORDER BY pjtCompletedDate")
    fun getAllCompletedProjects(): Flow<List<ProjectWithSubTasks>>

    // to get all projects with theirs stats
    @Query("SELECT * FROM project_table")
    fun getProjectsStats(): Flow<List<Project>>

    // to get one particular project
    @Transaction
    @Query("SELECT * FROM project_table WHERE p_id = :projectId LIMIT 1")
    suspend fun getProject(projectId: Long): ProjectWithSubTasks

    @Query("SELECT COUNT(pjtName) FROM project_table WHERE (pjtStartDate > :endOfToday OR pjtDeadline > :endOfToday ) AND is_pjt_completed == 0")
    suspend fun getUpcomingProjectsCount(endOfToday: Long): Int
}