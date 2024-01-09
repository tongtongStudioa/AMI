package com.tongtongstudio.ami.data.dao

import androidx.room.*
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    // TODO: change this to implement spread event
    @Query("SELECT * FROM event_table WHERE event_deadline BETWEEN :startOfToday AND :endOfToday AND is_event_completed = :hideCompleted")
    fun getTodayEvents(
        hideCompleted: Boolean,
        startOfToday: Long,
        endOfToday: Long
    ): Flow<List<Event>>

    fun getAllEvents(hideCompleted: Boolean, sortOrder: SortOrder): Flow<List<Event>> =
        when (sortOrder) {
            SortOrder.BY_EISENHOWER_MATRIX -> getEventsByPriority(hideCompleted)
            SortOrder.BY_DEADLINE -> getEventsByDeadline(
                hideCompleted
            )
            SortOrder.BY_NAME -> getEventsByName(hideCompleted)
            else -> getEventsByDeadline(hideCompleted)
        }

    @Query("SELECT * FROM event_table WHERE is_event_completed = :hideCompleted ORDER BY event_name")
    fun getEventsByName(hideCompleted: Boolean): Flow<List<Event>>

    @Query("SELECT * FROM event_table WHERE is_event_completed = :hideCompleted ORDER BY event_priority")
    fun getEventsByPriority(hideCompleted: Boolean): Flow<List<Event>>

    @Query("SELECT * FROM event_table WHERE is_event_completed = :hideCompleted ORDER BY event_deadline")
    fun getEventsByDeadline(hideCompleted: Boolean): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: Event): Long

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)

    // TODO: 04/02/2023 test with beginning date and ending date
    @Query("SELECT * FROM event_table WHERE  event_deadline > :endOfToday ")
    fun getLaterEvents(endOfToday: Long): Flow<List<Event>>

    @Query("SELECT * FROM event_table WHERE  event_deadline  BETWEEN :endOfToday AND :endOfDayFilter")
    fun getLaterEventsFilter(endOfToday: Long, endOfDayFilter: Long): Flow<List<Event>>

    @Query("SELECT COUNT(event_name) FROM event_table WHERE event_start_date > :endOfToday OR event_deadline > :endOfToday")
    suspend fun getUpcomingEventsCount(endOfToday: Long): Int
}