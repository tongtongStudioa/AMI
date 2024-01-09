package com.tongtongstudio.ami.ui.edit

import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward

class CalendarCustomFunction {

    companion object {


        /**
         * Create a constraint point backward which exclude future date after maxDate
         * @param maxDate
         * @return constraints point backward
         */
        fun buildConstraintsForStartDate(maxDate: Long?): CalendarConstraints {
            val calendarConstraintsBuilder = CalendarConstraints.Builder()
            if (maxDate != null)
                calendarConstraintsBuilder.setValidator(DateValidatorPointBackward.before(maxDate))
            return calendarConstraintsBuilder.build()
        }

        fun buildConstraintsForDeadline(minDate: Long): CalendarConstraints {
            return CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.from(minDate))
                .build()
        }

        fun buildConstraintsForDueDate(minDate: Long, maxDate: Long?): CalendarConstraints {
            val calendarConstraintsBuilder = CalendarConstraints.Builder()
            calendarConstraintsBuilder.setValidator(DateValidatorPointForward.from(minDate))
            if (maxDate != null)
                calendarConstraintsBuilder.setValidator(DateValidatorPointBackward.before(maxDate))
            return calendarConstraintsBuilder.build()
        }
    }
}