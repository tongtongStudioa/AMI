package com.tongtongstudio.ami.data.datatables

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.util.*

internal class TtdTest {
    @Test
    fun getHabitSuccessRate_noSuccess_returnNull() {
        val ttd = Ttd("test", 1, dueDate = Calendar.getInstance().timeInMillis, isRecurring = false)

        val result = ttd.getHabitSuccessRate()

        assertEquals(result, null)
    }
}