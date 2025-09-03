package com.tongtongstudio.ami.data.datatables

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

internal class TtdTest {
    @Test
    fun getHabitSuccessRate_noSuccess_returnNull() {
        val ttd =
            Task("test", 1, dueDate = Calendar.getInstance().timeInMillis)

        // TODO: create a real test for habit success rate : completed times / number times passed from start date
        val result = null

        assertEquals(result, null)
    }
}