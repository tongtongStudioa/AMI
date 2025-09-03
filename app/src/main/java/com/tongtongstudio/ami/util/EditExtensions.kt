package com.tongtongstudio.ami.util

import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Extensions.kt
fun EditText.setTextIfDifferent(newText: String) {
    if (text.toString() != newText) {
        setText(newText)
    }
}

fun MaterialButton.updateDateButton(date: Long?, defaultText: String) {
    if (date != null) {
        text = SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(date)
        setTextColor(MaterialColors.getColor(context, R.attr.colorPrimary,""))
    } else {
        text = defaultText
        setTextColor(MaterialColors.getColor(context, R.attr.colorPrimaryInverse,""))
    }
}

fun Fragment.showDatePicker(
    initialDate: Long? = null,
    constraints: CalendarConstraints? = null,
    onDateSelected: (Long) -> Unit
) {
    val datePicker = MaterialDatePicker.Builder.datePicker()
        .setSelection(initialDate ?: MaterialDatePicker.todayInUtcMilliseconds())
        .apply { constraints?.let { setCalendarConstraints(it) } }
        .build()

    datePicker.addOnPositiveButtonClickListener { date ->
        val selectedDate = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        onDateSelected(selectedDate)
    }

    datePicker.show(parentFragmentManager, "DATE_PICKER_TAG")
}