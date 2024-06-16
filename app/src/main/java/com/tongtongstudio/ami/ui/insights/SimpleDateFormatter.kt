package com.tongtongstudio.ami.ui.insights

import com.github.mikephil.charting.formatter.ValueFormatter
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import java.text.SimpleDateFormat
import java.util.*

class SimpleDateFormatter : ValueFormatter() {
    private val dateFormat = SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault())
    override fun getFormattedValue(value: Float): String {
        val date = Date(value.toLong())
        return dateFormat.format(date)
    }
}