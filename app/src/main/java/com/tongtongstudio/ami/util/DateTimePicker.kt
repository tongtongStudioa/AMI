package com.tongtongstudio.ami.util

import android.content.Context
import androidx.core.util.Pair
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import com.tongtongstudio.ami.R
import java.util.Calendar


class DateTimePicker(private val parentFragmentManager: FragmentManager, val context: Context) {

    fun showDialogNewReminder(
        dueDateTime: Long? = null,
        actionSaveReminder: (Long) -> Unit
    ) {
        var reminderTriggerTime: Long
        // create the calendar constraint builder
        val endDateConstraints = CalendarCustomFunction.buildConstraintsForDeadline(
            Calendar.getInstance().run {
                set(Calendar.HOUR_OF_DAY, 0)
                timeInMillis
            }
        )
        val reminderDatePicker = showDatePickerMaterial(endDateConstraints, dueDateTime)

        reminderDatePicker.addOnPositiveButtonClickListener { dateInMillisSelection ->
            val timePicker = showTimePickerMaterial(dueDateTime)
            timePicker.addOnPositiveButtonClickListener {
                val pickedHour = timePicker.hour
                val pickedMinutes = timePicker.minute
                reminderTriggerTime =
                    setReminderTriggerTime(dateInMillisSelection, pickedHour, pickedMinutes)
                actionSaveReminder(reminderTriggerTime)
            }
        }
    }

    private fun setReminderTriggerTime(date: Long, pickedHour: Int, pickedMinutes: Int): Long {
        return Calendar.getInstance().run {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, pickedHour)
            set(Calendar.MINUTE, pickedMinutes)
            timeInMillis
        }
    }

    fun showDatePickerMaterial(
        constraints: CalendarConstraints,
        selection: Long? = null
    ): MaterialDatePicker<Long> {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(context.getString(R.string.select_date))
                .setCalendarConstraints(constraints)
                .setSelection(selection ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()
        datePicker.show(parentFragmentManager, "datePicker")

        return datePicker
    }

    private fun showTimePickerMaterial(dueDateTime: Long? = null): MaterialTimePicker {
        val hour: Int? = (dueDateTime?.div((3600 * 1000)))?.toInt()
        val minute: Int? = (dueDateTime?.rem((3600 * 1000)))?.toInt()?.div(60 * 1000)
        val timePicker =
            MaterialTimePicker.Builder()
                .setTitleText(context.getString(R.string.select_reminder_time_title))
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour ?: (Calendar.getInstance().get(Calendar.HOUR) + 1))
                .setMinute(minute ?: 30)
                .setInputMode(INPUT_MODE_CLOCK)
                .build()
        timePicker.show(parentFragmentManager, "timePicker")

        return timePicker
    }

    private fun showDateRangePickerMaterial(): MaterialDatePicker<Pair<Long, Long>> {
        val dateRangePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(context.getString(R.string.Select_two_date_to_spread_event))
                .build()
        dateRangePicker.show(parentFragmentManager, "dateRangePicker")

        return dateRangePicker
    }

}