package com.tongtongstudio.ami.util

import java.util.*
import kotlin.random.Random

class Util {
    /**
     * Create a random date upcoming.
     * @return date in millis
     */
    fun getRdDate(): Long {
        val calendar = Calendar.getInstance()
        val dateInMillis = calendar.run {
            add(Calendar.DAY_OF_MONTH, Random.nextInt(1, 5))
            timeInMillis
        }
        return dateInMillis
    }

    fun getTimeInMillis(hours: Int = 1, minutes: Int = 0): Long =
        (hours * 3600 * 1000 + minutes * 60 * 1000).toLong()

    fun getRdPastDate(): Long {
        val calendar = Calendar.getInstance()
        val dateInMillis = calendar.run {
            add(Calendar.DAY_OF_MONTH, Random.nextInt(-8, -1))
            timeInMillis
        }
        return dateInMillis
    }
}
val <T> T.exhaustive: T
    get() = this