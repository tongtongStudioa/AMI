package com.tongtongstudio.ami.data.datatables

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.ThingToDoDatabase
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.util.DataTestUtil
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*


internal class TtdTest {

    private lateinit var taskDao: TaskDao
    private lateinit var db: ThingToDoDatabase
    private lateinit var dataTestUtil: DataTestUtil

    /**
     * Create and populate database.
     */
    @Before
    fun setUp() = runBlocking {
        // create db
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, ThingToDoDatabase::class.java
        ).build()

        // retrieve thing_to_do's DAO
        taskDao = db.taskDao()

        // populate db
        dataTestUtil = DataTestUtil.getInstance(taskDao)
        dataTestUtil.insertTestTasks()
    }

    @After
    @Throws(Exception::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun getAllTasks_EisenhowerMatrixSort_returnSortedList() = runBlocking {
        val resultedFlow = taskDao.getTodayTasks(
            SortOrder.BY_EISENHOWER_MATRIX,
            false,
            false,
            dataTestUtil.startOfDay,
            dataTestUtil.endOfDay
        )
        val result = resultedFlow.first()
        println("Data inserted:")
        dataTestUtil.getTasks().forEach { println(it.title) }

        println("Sorted data:")
        result.forEach { println(it.mainTask.title) }

        assertEquals(result, result.sortedWith(
            compareBy<ThingToDo> { it.mainTask.priority }
                .thenByDescending { it.mainTask.importance }
                .thenByDescending { it.mainTask.urgency }
                .thenByDescending { it.mainTask.estimatedWorkingTime })
        )
    }

    @Test
    fun getLaterTasks_laterInWeek_sortedList() = runBlocking {
        val calendar = Calendar.getInstance()
        val endOfDay = calendar.run {
            set(Calendar.HOUR, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            timeInMillis
        }
        //end of day in a week
        val endOfDayWeek = calendar.run {
            add(Calendar.DAY_OF_MONTH, 7)
            set(Calendar.HOUR, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            timeInMillis
        }

        val resultedFlow = taskDao.getLaterTasksFilter(endOfDay, endOfDayWeek)
        val result = resultedFlow.first()

        println("Result :")
        result.forEach { println(it.mainTask.title + " " + it.mainTask.dueDate) }

        assertEquals(result, result.sortedWith(
            compareBy<ThingToDo> { it.mainTask.dueDate }
                .thenBy { it.mainTask.deadline }
                .thenBy { it.mainTask.startDate }
                .thenBy { it.mainTask.priority }
                .thenByDescending { it.mainTask.estimatedWorkingTime }
        ))
    }

    @Test
    fun getAchievementRate_allTasks_correctRate() = runBlocking {

        val resultingRate = taskDao.getTotalAchievementRate()

        // actually rate must be 50.0 (%)
        assertEquals(50.0F, resultingRate)

    }

    @Test
    fun getHabitCompletionRate_allRecurringTasks_correctRate() = runBlocking {

        val resultingRate = taskDao.getHabitCompletionRate()

        // actually rate must be 66.7 (%)
        assertEquals(66.7F, resultingRate)

    }

    @Test
    fun getOnTimeCompletionRate_allCompletedTasks_correctRate() = runBlocking {

        val resultingRate = taskDao.getOnTimeCompletionTasksRate()

        // actually rate must be 25.0 (%)
        assertEquals(25.0F, resultingRate)
    }

    @Test
    fun getEstimationAccuracyRate_allCompletedTasks_correctRate() = runBlocking {

        val resultingRate = taskDao.getAccuracyRateOfEstimatedWorkTime(0.2F)

        // actually rate must be 25.0 (%)
        assertEquals(25.0F, resultingRate)

    }


    @Test
    fun getLaterTasks_tomorrow_listWithoutUnexpectedTasks() = runBlocking {
        val calendar = Calendar.getInstance()
        val endOfDay = calendar.run {
            set(Calendar.HOUR, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            timeInMillis
        }
        //end of day tomorrow
        val endOfDayTomorrow = calendar.run {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            timeInMillis
        }

        val resultedFlow = taskDao.getLaterTasksFilter(endOfDay, endOfDayTomorrow)
        val result = resultedFlow.first()

        val list = dataTestUtil.getTasks()
        val tasksList = ArrayList<Task>()
        for (task in list) {
            if ((task.dueDate != null && task.dueDate!! < endOfDayTomorrow || task.startDate != null && task.startDate!! < endOfDayTomorrow) && task.dueDate != null && task.dueDate!! > endOfDay && task.startDate != null && task.startDate!! > endOfDay) {
                tasksList.add(task)
            }
        }

        assertEquals(result.size, tasksList.size)
    }
}