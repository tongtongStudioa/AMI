package com.tongtongstudio.ami.util

import com.tongtongstudio.ami.data.dao.TtdDao
import com.tongtongstudio.ami.data.datatables.Ttd
import java.util.*

class DataTestUtil(private val ttdDao: TtdDao) {

    val util = Util()

    private val tasks = listOf(
        Ttd(
            "Faire les courses",
            4,
            util.getRdDate(),
            importance = 4,
            urgency = 8,
            estimatedTime = util.getTimeInMillis(1, 30),
            dependency = false,
            skillLevel = 10
        ),
        Ttd(
            "Examen de python",
            1,
            util.getRdDate(),
            importance = 9,
            urgency = 2,
            estimatedTime = util.getTimeInMillis(4, 0),
            dependency = false,
            skillLevel = 4
        ),
        Ttd(
            "Rdv dentiste",
            2,
            util.getRdDate(),
            importance = 6,
            urgency = 10,
            estimatedTime = util.getTimeInMillis(0, 30),
            dependency = true
        ),
        Ttd(
            "Aller voir un pote",
            4,
            util.getRdDate(),
            importance = 4,
            urgency = 8,
            estimatedTime = util.getTimeInMillis(2, 30),
            dependency = true,
            skillLevel = 10
        ),
        Ttd(
            "Créer un test pour la base de donnée",
            4,
            util.getRdPastDate(),
            importance = 4,
            urgency = 8,
            estimatedTime = util.getTimeInMillis(1, 30),
            dependency = false,
            skillLevel = 10,
            isCompleted = true,
            actualWorkTime = util.getTimeInMillis(1),
            completedOnTime = true
        ),
        Ttd(
            "Examen de math",
            1,
            util.getRdPastDate(),
            importance = 9,
            urgency = 2,
            estimatedTime = util.getTimeInMillis(4, 0),
            dependency = false,
            skillLevel = 4,
            isCompleted = true,
            actualWorkTime = util.getTimeInMillis(4, 0),
            completedOnTime = true
        ),
        Ttd(
            "Faire une lessive",
            2,
            util.getRdPastDate(),
            importance = 6,
            urgency = 10,
            estimatedTime = util.getTimeInMillis(0, 30),
            dependency = true,
            isCompleted = true,
            actualWorkTime = util.getTimeInMillis(2),
            completedOnTime = false
        ),
        Ttd(
            "Boire de l'eau",
            4,
            util.getRdPastDate(),
            importance = 4,
            urgency = 8,
            estimatedTime = util.getTimeInMillis(2, 30),
            dependency = true,
            skillLevel = 10,
            isCompleted = true,
            actualWorkTime = util.getTimeInMillis(3, 40),
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

    fun getTasks(): List<Ttd> = tasks
    fun getTasksListSize(): Int = tasks.size

    suspend fun insertTestTasks() {
        ttdDao.insertTasks(tasks)
    }

    companion object {
        fun getInstance(ttdDao: TtdDao): DataTestUtil {
            return DataTestUtil(ttdDao)
        }
    }
}