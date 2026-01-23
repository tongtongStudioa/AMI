package com.tongtongstudio.ami.data.datatables

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tongtongstudio.ami.data.ThingToDoDatabase
import com.tongtongstudio.ami.data.dao.TaskCompletionDao
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
import kotlin.collections.filter


internal class TtdTest {

    private lateinit var taskDao: TaskDao
    private lateinit var taskCompletionDao: TaskCompletionDao
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
        taskCompletionDao = db.taskCompletionDao()

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
    fun getSubTasks_returnSubTasksList() = runBlocking {
        val subTasks = dataTestUtil.getTasks().filter {
            it.parentTaskId != null
        }
        print("Sub tasks : ")
        subTasks.forEach { print(it.title+ ", ") }

        val result = taskDao.getSubTasks(5).first()
        assert(result.size == subTasks.size) {"Problem : not same size list; result list size = ${result.size} / subtasks test data = ${subTasks.size}"}

    }

    @Test
    @Throws(IOException::class)
    fun getTasksRelations_allTasksWithRelations_returnTaskRelations() = runBlocking {
        val tasks = dataTestUtil.getTasks()
        print("Tasks : ")
        tasks.forEach { print(it.title+ ", ") }

        val result = taskDao.getTasksRelations().first()
        result.forEach { println(it) }
        assert(result.size == tasks.size) {"Problem : not same size list; result list size = ${result.size} / subtasks test data = ${tasks.size}"}
    }

    @Test
    @Throws(IOException::class)
    fun getAllTasks_EisenhowerMatrixSort_returnSortedList() = runBlocking {

        println("Data inserted:")
        val tasks = dataTestUtil.getTasks().filter {
            it.startDate?.let {
                it > dataTestUtil.startOfDay && it < dataTestUtil.endOfDay
            } == true || it.dueDate?.let {
                it < dataTestUtil.endOfDay
            } == true || it.deadline?.let {
                it > dataTestUtil.startOfDay && it < dataTestUtil.endOfDay
            } == true
        }
        tasks.forEach { print(it.title + ", ") }
        val resultedFlow = taskDao.getTasksOrderByEisenhowerMatrixSort(
            false,
            false,
            dataTestUtil.startOfDay,
            dataTestUtil.endOfDay
        )

        val result = resultedFlow.first()
        println("Sorted data:")
        result.forEach { println(it) }

        assert(tasks.size == result.size) {" Not same size list : result list size = ${result.size} / tasks test data = ${tasks.size}"}
        assertEquals(result, result.sortedWith(
            compareByDescending <ThingToDo> { it.taskRelations.mainTask.priority }
                .thenByDescending { it.taskRelations.mainTask.importance }
                .thenByDescending { it.taskRelations.mainTask.urgency }
                .thenByDescending { it.taskRelations.mainTask.estimatedWorkingTime })
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

        val resultedFlow = taskDao.getLaterTasks(endOfDay, endOfDayWeek)
        val result = resultedFlow.first()

        println("Result :")
        result.forEach { println(it.taskRelations.mainTask.title + " " + it.taskRelations.mainTask.dueDate) }

        assertEquals(result, result.sortedWith(
            compareBy<ThingToDo> { it.taskRelations.mainTask.dueDate }
                .thenBy { it.taskRelations.mainTask.deadline }
                .thenBy { it.taskRelations.mainTask.startDate }
                .thenBy { it.taskRelations.mainTask.priority }
                .thenByDescending { it.taskRelations.mainTask.estimatedWorkingTime }
        ))
    }

    @Test
    fun getAchievementRate_allTasks_correctRate() = runBlocking {

        val resultingRate = taskDao.getAchievementRate()

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

        val resultingRate: Float = taskDao.getAccuracyRateOfEstimatedWorkTime(0.2F,1)

        // actually rate must be 25.0 (%)
        assertEquals(25.0F, resultingRate)

    }

    @Test
    fun testTaskCompletionStreak() = runBlocking {
        val task = Task(title = "Task Streak", dueDate = null, priority = 1)
        val taskId = taskDao.insert(task)

        val today = System.currentTimeMillis()
        val yesterday = today - (24 * 60 * 60 * 1000)

        taskCompletionDao.insert(TaskCompletion(taskId = taskId, isCompleted = true, completionDate = yesterday))
        taskCompletionDao.insert(TaskCompletion(taskId = taskId, isCompleted = true, completionDate = today))

        val streak = taskDao.getCurrentStreakByTask(taskId)
        assertEquals(2, streak)
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

        val resultedFlow = taskDao.getLaterTasks(endOfDay, endOfDayTomorrow)
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