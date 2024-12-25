package com.tongtongstudio.ami.util

import com.tongtongstudio.ami.data.dao.TaskDao
import com.tongtongstudio.ami.data.datatables.Task
import java.util.Calendar

class DataTestUtil(private val ttdDao: TaskDao) {

    val util = Util()

    private val tasks = listOf(
        Task(
            "Faire les courses",
            4,
            util.getRdDate(),
            importance = 4,
            urgency = 8,
            estimatedWorkingTime = util.getTimeInMillis(1, 30),
            dependency = false,
            skillLevel = 10
        ),
        Task(
            "Examen de python",
            1,
            util.getRdDate(),
            importance = 9,
            urgency = 2,
            estimatedWorkingTime = util.getTimeInMillis(4, 0),
            dependency = false,
            skillLevel = 4
        ),
        Task(
            "Rdv dentiste",
            2,
            util.getRdDate(),
            importance = 6,
            urgency = 10,
            estimatedWorkingTime = util.getTimeInMillis(0, 30),
            dependency = true
        ),
        Task(
            "Aller voir un pote",
            4,
            util.getRdDate(),
            importance = 4,
            urgency = 8,
            estimatedWorkingTime = util.getTimeInMillis(2, 30),
            isRecurring = true,
            dependency = true,
            skillLevel = 10
        ),
        Task(
            "Créer un test pour la base de donnée",
            4,
            util.getRdPastDate(),
            importance = 4,
            urgency = 8,
            estimatedWorkingTime = util.getTimeInMillis(1, 30),
            dependency = false,
            skillLevel = 10,
            isCompleted = true,
            currentWorkingTime = util.getTimeInMillis(1),
            completedOnTime = true
        ),
        Task(
            "Examen de math",
            1,
            util.getRdPastDate(),
            importance = 9,
            urgency = 2,
            estimatedWorkingTime = util.getTimeInMillis(4, 0),
            dependency = false,
            skillLevel = 4,
            isCompleted = true,
            currentWorkingTime = util.getTimeInMillis(4, 0),
            completedOnTime = false
        ),
        Task(
            "Faire une lessive",
            2,
            util.getRdPastDate(),
            importance = 6,
            urgency = 10,
            estimatedWorkingTime = util.getTimeInMillis(0, 30),
            dependency = true,
            isCompleted = true,
            currentWorkingTime = util.getTimeInMillis(2),
            completedOnTime = false
        ),
        Task(
            "Boire de l'eau",
            4,
            util.getRdPastDate(),
            importance = 4,
            urgency = 8,
            estimatedWorkingTime = util.getTimeInMillis(2, 30),
            dependency = true,
            skillLevel = 10,
            isCompleted = true,
            currentWorkingTime = util.getTimeInMillis(3, 40),
            completedOnTime = false
        )

    )

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

    suspend fun insertTestTasks() {
        ttdDao.insertTasks(tasks)
    }

    companion object {
        fun getInstance(ttdDao: TaskDao): DataTestUtil {
            return DataTestUtil(ttdDao)
        }
    }
}