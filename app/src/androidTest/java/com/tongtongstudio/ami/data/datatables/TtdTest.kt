package com.tongtongstudio.ami.data.datatables

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tongtongstudio.ami.data.ThingToDoDatabase1
import com.tongtongstudio.ami.data.dao.TtdDao
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

    private lateinit var ttdDao: TtdDao
    private lateinit var db: ThingToDoDatabase1
    private lateinit var dataTestUtil: DataTestUtil

    /**
     * Create and populate database.
     */
    @Before
    fun setUp() = runBlocking {
        // create db
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, ThingToDoDatabase1::class.java
        ).build()

        // retrieve thing_to_do's DAO
        ttdDao = db.ttdDao()

        // populate db
        dataTestUtil = DataTestUtil.getInstance(ttdDao)
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
        val resultedFlow = ttdDao.getTodayTasks(
            "EisenhowerMatrixSort",
            false,
            dataTestUtil.startOfDay,
            dataTestUtil.endOfDay
        )
        val result = resultedFlow.first()
        println("Data inserted:")
        dataTestUtil.getTasks().forEach { println(it.title) }

        println("Sorted data:")
        result.forEach { println(it.title) }

        assertEquals(result, result.sortedWith(
            compareBy<Ttd> { it.priority }
                .thenByDescending { it.importance }
                .thenByDescending { it.urgency }
                .thenByDescending { it.estimatedTime })
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

        val resultedFlow = ttdDao.getLaterTasks(endOfDay, endOfDayWeek)
        val result = resultedFlow.first()

        println("Result :")
        result.forEach { println(it.title + " " + it.dueDate) }

        assertEquals(result, result.sortedWith(
            compareBy<Ttd> { it.dueDate }
                .thenBy { it.deadline }
                .thenBy { it.startDate }
                .thenBy { it.priority }
                .thenByDescending { it.estimatedTime }
        ))
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

        val resultedFlow = ttdDao.getLaterTasks(endOfDay, endOfDayTomorrow)
        val result = resultedFlow.first()

        val list = dataTestUtil.getTasks()
        val tasksList = ArrayList<Ttd>()
        for (task in list) {
            if ((task.dueDate < endOfDayTomorrow || task.startDate != null && task.startDate!! < endOfDayTomorrow) && task.dueDate > endOfDay && task.startDate != null && task.startDate!! > endOfDay) {
                tasksList.add(task)
            }
        }

        assertEquals(result.size, tasksList.size)

    }
}
