package com.tongtongstudio.ami.data.datatables

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tongtongstudio.ami.data.ThingToDoDatabase
import com.tongtongstudio.ami.data.dao.TaskCompletionDao
import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.dao.WorkSessionDao
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
    private lateinit var workSessionDao: WorkSessionDao
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
        workSessionDao = db.workSession()
        // populate db
        dataTestUtil = DataTestUtil.getInstance(taskDao)
        dataTestUtil.insertTestTasks()
        dataTestUtil.taskCompletions.forEach {
            taskCompletionDao.insert(it)
        }
        dataTestUtil.workSessions.forEach {
            workSessionDao.insert(it)
        }
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

        val result = taskDao.getSubTasks(5).first()
        assertEquals("Problem : not same size list; result list size = ${result.size} / subtasks test data = ${subTasks.size}",
            subTasks.size,result.size)

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
        val thingsToDo = dataTestUtil.getThingsToDo().filterIndexed { index, thingToDo ->
            thingToDo.taskRelations.mainTask.startDate?.let {
                it > dataTestUtil.startOfDay && it < dataTestUtil.endOfDay
            } == true || thingToDo.taskRelations.mainTask.dueDate?.let {
                it < dataTestUtil.endOfDay && it > dataTestUtil.startOfDay
            } == true || thingToDo.taskRelations.mainTask.dueDate?.let {
                it < dataTestUtil.endOfDay && !dataTestUtil.taskCompletions[index].isCompleted
            } == true || thingToDo.taskRelations.mainTask.deadline?.let {
                it > dataTestUtil.startOfDay && it < dataTestUtil.endOfDay
            } == true
        }.sortedWith(
            compareByDescending <ThingToDo> { it.taskRelations.mainTask.priority }
                .thenByDescending { it.taskRelations.mainTask.importance }
                .thenByDescending { it.taskRelations.mainTask.urgency }
                .thenByDescending { it.taskRelations.mainTask.estimatedWorkingTime })

        thingsToDo.forEach { print(it.taskRelations.mainTask.title + ", ") }
        val resultedFlow = taskDao.getTasksOrderByEisenhowerMatrixSort(
            false,
            false,
            dataTestUtil.startOfDay,
            dataTestUtil.endOfDay
        )

        val result = resultedFlow.first()
        println("Sorted data:")
        result.forEach { println(it) }

        assert(thingsToDo.size == result.size) {" Not same size list : result list size = ${result.size} / tasks test data = ${thingsToDo.size}"}
        assertEquals(thingsToDo.map { it.taskRelations.mainTask.title }, result.map { it.taskRelations.mainTask.title })
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

        val thingsToDoRef = dataTestUtil.getThingsToDo()
            .filter { it.taskRelations.mainTask.dueDate!! < endOfDayWeek && it.taskRelations.mainTask.dueDate > endOfDay}
            .filter { it.lastCompletionStatus == false }
            .sortedWith(
                compareBy<ThingToDo> { it.taskRelations.mainTask.dueDate }
                    .thenBy { it.taskRelations.mainTask.deadline }
                    .thenBy { it.taskRelations.mainTask.startDate }
                    .thenBy { it.taskRelations.mainTask.priority }
                    .thenByDescending { it.taskRelations.mainTask.estimatedWorkingTime }
            ).map { it.taskRelations.mainTask.title }

        println("Result :")
        result.forEach { println(it.taskRelations.mainTask.title) }

        assertEquals(thingsToDoRef, result.map { it.taskRelations.mainTask.title },
            )
    }

    @Test
    fun getAchievementRate_allTasks_correctRate() = runBlocking {

        val resultingRate = taskDao.getAchievementRate()
        val rateRef = dataTestUtil.taskCompletions.count { it.isCompleted }.toFloat() / dataTestUtil.taskCompletions.count() * 100
        assertEquals(rateRef, resultingRate,0.01f)

    }

    @Test
    fun getHabitCompletionRate_allRecurringTasks_correctRate() = runBlocking {

        val resultingRate = taskDao.getHabitCompletionRate()
        val countRecurrentTask = dataTestUtil.getTasks().count { it.recurrenceInfosId != null }
        val rateRef = dataTestUtil.getTasks().filter { it.recurrenceInfosId != null }.map { task ->
            val count = dataTestUtil.taskCompletions.count { it.id == task.recurrenceInfosId }
            dataTestUtil.taskCompletions.filter { it.id ==  task.recurrenceInfosId }.count {it.isCompleted}.toDouble() / if (count != 0) count else 1

        }.sumOf { it }.toFloat() / if (countRecurrentTask != 0) countRecurrentTask else 1

        assertEquals(rateRef, resultingRate,0.01f)

    }

    @Test
    fun getOnTimeCompletionRate_allCompletedTasks_correctRate() = runBlocking {

        val resultingRate = taskDao.getOnTimeCompletionTasksRate()
        val rateRef = dataTestUtil.getTasks().filterIndexed { index, task ->
            Log.i("ON TIME COMPLETION RATE", (dataTestUtil.taskCompletions[index].isCompleted && task.dueDate!! >= dataTestUtil.taskCompletions[index].completionDate).toString())
            dataTestUtil.taskCompletions[index].isCompleted && task.dueDate!! >= dataTestUtil.taskCompletions[index].completionDate
        }.count() / dataTestUtil.getTasks().filterIndexed {index, task -> dataTestUtil.taskCompletions[index].isCompleted }.count().toFloat() * 100

        assertEquals(rateRef, resultingRate,0.0002f)
    }

    @Test
    fun getEstimationAccuracyRate_allCompletedTasks_correctRate() = runBlocking {

        val resultingRate: Float = taskDao.getAccuracyRateOfEstimatedWorkTime(0.2F)
        val rateRef = dataTestUtil.getTasks().map { it.estimatedWorkingTime }.filterIndexed { index, estimatedTime -> dataTestUtil.workSessions[index].duration < estimatedTime!! * 1.2 && dataTestUtil.workSessions[index].duration >  estimatedTime * 0.8 }.count() / dataTestUtil.getTasksListSize().toFloat() * 100
        assertEquals(rateRef, resultingRate,0.001f)

    }

    @Test
    fun taskCompletionStreak_correctRate() = runBlocking {
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

        val thingsToDoRef = dataTestUtil.getTasks().filterIndexed {index,  task ->
            !dataTestUtil.taskCompletions[index].isCompleted && ((task.dueDate != null && task.dueDate < endOfDayTomorrow && task.dueDate > endOfDay) || (task.startDate != null && task.startDate > endOfDay && task.startDate < endOfDayTomorrow))
        }

        assertEquals(thingsToDoRef.size, result.size)
    }
}