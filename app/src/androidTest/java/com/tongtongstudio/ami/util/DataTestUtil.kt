package com.tongtongstudio.ami.util

import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.TaskCompletion
import com.tongtongstudio.ami.data.datatables.TaskRelations
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.WorkSession
import java.util.Calendar
import kotlin.random.Random
import kotlin.random.nextInt

class DataTestUtil(private val ttdDao: TaskDao) {

    val util = Util()

    fun createRdTask(index: Int) = Task(
        "Task $index",
        Random.nextInt(1, 10),
        util.getRdDate(),
        importance = Random.nextInt(1, 10),
        urgency = Random.nextInt(1, 10),
        estimatedWorkingTime = util.getTimeInMillis(Random.nextInt(0, 5), 30),
        skillLevel = 10,
        id = index.toLong()
    )

    fun createTaskCompletion(taskId: Long, taskDueDate: Long): TaskCompletion {
        val isCompleted = Random.nextBoolean()
        val completionDate = Calendar.getInstance().run {
            timeInMillis = taskDueDate
            add(Calendar.DAY_OF_MONTH,Random.nextInt(-5,5))
            timeInMillis
        }
        return TaskCompletion(
            taskId = taskId,
            isCompleted = isCompleted,
            completionDate = completionDate
        )
    }

    fun createWorkSession(taskId: Long, taskDueDate: Long, taskEstimatedTime: Long) = WorkSession(
        parentTaskId = taskId,
        duration = taskEstimatedTime + (Random.nextInt(-1..1) * taskEstimatedTime).toLong(),
        comment = null,
        date = Calendar.getInstance().run {
            timeInMillis = taskDueDate
            add(Calendar.DAY_OF_MONTH, Random.nextInt(-3,0))
            timeInMillis
        }
    )

    private val tasks = buildList {
        for (i in 1..10) {
            add(createRdTask(i))
        }
    }

    val taskCompletions: List<TaskCompletion> = buildList {
        for (task in tasks) {
            add(createTaskCompletion(task.id,task.dueDate!!))
        }
    }

    val workSessions: List<WorkSession> = buildList {
        for (task in tasks){
            add(createWorkSession(task.id, task.dueDate!!, task.estimatedWorkingTime!!))
        }
    }
    val startOfDay: Long = Calendar.getInstance().run {
        set(Calendar.HOUR, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        timeInMillis
    }

    val endOfDay: Long = Calendar.getInstance().run {
        set(Calendar.HOUR, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        timeInMillis
    }

    fun getTasks(): List<Task> = tasks
    fun getTasksListSize(): Int = tasks.size

    fun getThingsToDo(): List<ThingToDo> = buildList {
        tasks.forEachIndexed { index, task ->
            add(ThingToDo(
                TaskRelations(
                    mainTask = task, taskDependency = null, category = null,
                    parentProject = null
                ),
                totalMainSubTtd = 0,
                nbSubTasks = 0,
                nbSubTasksCompleted = 0,
                lastCompletionStatus = taskCompletions[index].isCompleted,
                completionRate = 0f,
            )
            )
        }
    }

    suspend fun insertTestTasks() {
        ttdDao.insertTasks(tasks)
    }

    companion object {
        fun getInstance(ttdDao: TaskDao): DataTestUtil {
            return DataTestUtil(ttdDao)
        }
    }
}