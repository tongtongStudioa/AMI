package com.tongtongstudio.ami.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.RecurringTaskInterval
import com.tongtongstudio.ami.databinding.DialogSelectRecurringProtocolBinding
import java.util.*

enum class Period { DAYS, WEEKS, MONTHS, YEARS }

const val RECURRING_SELECTION_DIALOG_TAG = "recurring_selection_tag"
const val RECURRING_RESULT_KEY = "recurring_selection_result_key"
const val RECURRING_REQUEST_KEY = "recurring_selection_request_key"
const val CURRENT_RECURRING_INFO_REQUEST_KEY = "current_recurring_info_request_key"
const val TIMES_KEY = "times"
const val PERIOD_KEY = "period"
const val DAYS_OF_THE_WEEKS_KEY = "daysOfWeek"
const val DEADLINE = "deadline"
const val START_DATE = "start_date"
const val NO_VALUE = "no_value"

class RecurringChoiceDialogFragment : DialogFragment() {

    private lateinit var binding: DialogSelectRecurringProtocolBinding
    private var selection: Int = 0
    private lateinit var stringItems: Array<String>
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogSelectRecurringProtocolBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(binding.root)
                .setTitle(R.string.repeat_every_title)
                // Add action buttons
                .setPositiveButton(R.string.ok) { dialog, id ->
                    onDialogPositiveClick(this)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, id ->
                    getDialog()?.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onDialogPositiveClick(dialog: RecurringChoiceDialogFragment) {
        val nDays: Int =
            if (binding.inputLayoutUserChoice.editText?.text?.isNotEmpty() == true) {
                binding.inputLayoutUserChoice.editText?.text.toString().toInt()
            } else 1
        val period = getPeriod(selection)
        val recurrenceDays = getDaysOfWeek()
        val recurringTaskInterval = RecurringTaskInterval(nDays, period, recurrenceDays)
        val result = Bundle().apply {
            putParcelable(RECURRING_RESULT_KEY, recurringTaskInterval)
        }

        dialog.setFragmentResult(
            RECURRING_REQUEST_KEY,
            result
        )
        dialog.dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        stringItems = resources.getStringArray(R.array.period_list)
        val adapter = ArrayAdapter(requireContext(), R.layout.list_options, stringItems)

        setFragmentResultListener(CURRENT_RECURRING_INFO_REQUEST_KEY) { _, bundle ->
            val times = bundle.getInt(TIMES_KEY)
            val period = bundle.getString(PERIOD_KEY)
            val daysOfWeek = bundle.getIntArray(DAYS_OF_THE_WEEKS_KEY)
            val deadline = bundle.getString(DEADLINE)
            val startDate = bundle.getLong(START_DATE)
            // populate data
            selection = getSelection(period)
            binding.deadlineTextView.text =
                if (deadline == null || deadline == NO_VALUE) getString(R.string.set_recurring_end) else deadline
            binding.inputLayoutUserChoice.editText?.setText(if (times != 0) times.toString() else "1")
            // TODO: change text display with which period is already selected
            binding.autoCompleteTextView.setText(setPeriod(period))
            binding.autoCompleteTextView.setAdapter(adapter)
            if (period == Period.WEEKS.name)
                binding.daysOfWeekSelection.isVisible = true
            updateCheckBoxes(daysOfWeek, startDate)
        }

        binding.apply {
            autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
                selection = position
                binding.daysOfWeekSelection.isVisible = position == 1
            }
        }

        return binding.root
    }

    private fun getSelection(period: String?): Int {
        return when (period) {
            Period.DAYS.name -> 0
            Period.WEEKS.name -> 1
            Period.MONTHS.name -> 2
            Period.YEARS.name -> 3
            else -> 0
        }
    }

    private fun updateCheckBoxes(daysOfWeek: IntArray?, startDate: Long) {
        if (daysOfWeek != null) {
            for (day in daysOfWeek) {
                when (day) {
                    Calendar.MONDAY -> binding.mondayCheckBox.isChecked = true
                    Calendar.TUESDAY -> binding.tuesdayCheckBox.isChecked = true
                    Calendar.WEDNESDAY -> binding.wednesdayCheckBox.isChecked = true
                    Calendar.THURSDAY -> binding.thursdayCheckBox.isChecked = true
                    Calendar.FRIDAY -> binding.fridayCheckBox.isChecked = true
                    Calendar.SATURDAY -> binding.saturdayCheckBox.isChecked = true
                    Calendar.SUNDAY -> binding.sundayCheckBox.isChecked = true
                }
            }
        } else if (startDate != 0L) {
            when (Calendar.getInstance().run {
                timeInMillis = startDate
                get(Calendar.DAY_OF_WEEK)
            }) {
                Calendar.MONDAY -> binding.mondayCheckBox.isChecked = true
                Calendar.TUESDAY -> binding.tuesdayCheckBox.isChecked = true
                Calendar.WEDNESDAY -> binding.wednesdayCheckBox.isChecked = true
                Calendar.THURSDAY -> binding.thursdayCheckBox.isChecked = true
                Calendar.FRIDAY -> binding.fridayCheckBox.isChecked = true
                Calendar.SATURDAY -> binding.saturdayCheckBox.isChecked = true
                Calendar.SUNDAY -> binding.sundayCheckBox.isChecked = true
            }
        }
    }

    private fun getPeriod(listSelection: Int): String {
        return when (listSelection) {
            0 -> Period.DAYS.name
            1 -> Period.WEEKS.name
            2 -> Period.MONTHS.name
            3 -> Period.YEARS.name
            else -> Period.DAYS.name
        }
    }

    private fun setPeriod(period: String?): String {
        return when (period) {
            Period.DAYS.name -> stringItems[0]
            Period.WEEKS.name -> stringItems[1]
            Period.MONTHS.name -> stringItems[2]
            Period.YEARS.name -> stringItems[3]
            else -> stringItems[0]
        }
    }

    private fun getDaysOfWeek(): List<Int>? {
        val recurrenceDays = ArrayList<Int>()
        if (binding.mondayCheckBox.isChecked) recurrenceDays.add(Calendar.MONDAY)
        if (binding.tuesdayCheckBox.isChecked) recurrenceDays.add(Calendar.TUESDAY)
        if (binding.wednesdayCheckBox.isChecked) recurrenceDays.add(Calendar.WEDNESDAY)
        if (binding.thursdayCheckBox.isChecked) recurrenceDays.add(Calendar.THURSDAY)
        if (binding.fridayCheckBox.isChecked) recurrenceDays.add(Calendar.FRIDAY)
        if (binding.saturdayCheckBox.isChecked) recurrenceDays.add(Calendar.SATURDAY)
        if (binding.sundayCheckBox.isChecked) recurrenceDays.add(Calendar.SUNDAY)
        return recurrenceDays.ifEmpty { null }
    }
}