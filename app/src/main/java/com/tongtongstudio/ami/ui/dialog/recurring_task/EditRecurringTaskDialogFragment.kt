package com.tongtongstudio.ami.ui.dialog.recurring_task

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.view.iterator
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.databinding.DialogSetRepeatingBinding
import com.tongtongstudio.ami.util.InputValidation
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

enum class Period { DAYS, WEEKS, MONTHS, YEARS }

const val RECURRING_SELECTION_DIALOG_TAG = "recurring_selection_tag"
const val RECURRING_RESULT_KEY = "recurring_selection_result_key"
const val RECURRING_REQUEST_KEY = "recurring_selection_request_key"
const val CURRENT_RECURRING_INFO_REQUEST_KEY = "current_recurring_info_request_key"
const val TASK_RECURRENCE_WITH_DAYS_KEY = "task_recurrence_with_days"

@AndroidEntryPoint
class EditRecurringDialogFragment : DialogFragment() {

    private lateinit var binding: DialogSetRepeatingBinding
    private val viewModel: EditRecurringTaskViewModel by viewModels()
    lateinit var adapter: ArrayAdapter<String>

    private lateinit var stringItems: Array<String>
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogSetRepeatingBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(binding.root)
                .setTitle(R.string.repeat_every_title)
                // Add action buttons
                .setPositiveButton(R.string.ok) { dialog, id ->
                    onDialogPositiveClick(this, viewModel.uiState.value)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, id ->
                    getDialog()?.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onDialogPositiveClick(
        dialog: EditRecurringDialogFragment,
        uiState: EditRecurringTaskViewModel.EditRecurringTaskUiState
    ) {
        val taskRecurrenceWithDays = viewModel.buildTaskRecurrenceWithDays(uiState)
        val result = Bundle().apply {
            putParcelable(RECURRING_RESULT_KEY, taskRecurrenceWithDays)
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
        adapter = ArrayAdapter(requireContext(), R.layout.item_options, stringItems)

        setFragmentResultListener(CURRENT_RECURRING_INFO_REQUEST_KEY) { _, bundle ->
            val taskRecurrenceWithDays =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bundle.getParcelable(
                        TASK_RECURRENCE_WITH_DAYS_KEY,
                        TaskRecurrenceWithDays::class.java
                    )
                } else {
                    bundle.getParcelable(TASK_RECURRENCE_WITH_DAYS_KEY)
                }
            viewModel.updateTaskRecurrenceWithDays(taskRecurrenceWithDays)
            // populate data
            binding.inputLayoutInterval.editText?.setText(if (taskRecurrenceWithDays?.taskRecurrence?.interval != null) taskRecurrenceWithDays.taskRecurrence.interval.toString() else "1")
            val currentFrequency =
                adapter.getItem(getFrequencySelection(taskRecurrenceWithDays?.taskRecurrence?.frequency))
            binding.autoCompleteTextView.setText(currentFrequency)
            binding.autoCompleteTextView.setAdapter(adapter)
            if (taskRecurrenceWithDays?.taskRecurrence?.frequency == Period.WEEKS.name)
                binding.daysOfWeekSelection.isVisible = true
            updateCheckBoxes(
                taskRecurrenceWithDays?.daysOfWeek?.map { it.dayId }?.toIntArray()
                    ?: emptyArray<Int>().toIntArray(),
                taskRecurrenceWithDays?.taskRecurrence?.startDate
                    ?: Calendar.getInstance().timeInMillis
            )
            binding.deadlineTextView.text =
                if (taskRecurrenceWithDays?.taskRecurrence?.endDate == null) getString(R.string.set_recurring_end) else taskRecurrenceWithDays.taskRecurrence.endDate.toString()
        }

        binding.apply {
            autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
                binding.daysOfWeekSelection.isVisible = position == 1
                viewModel.updateFrequency(position)
                if (position != 1)
                    unCheckAllDays()
            }
            inputLayoutInterval.editText?.doOnTextChanged { intervalText, _, _, _ ->
                if (InputValidation.isValidText(intervalText))
                    viewModel.updateInterval(intervalText.toString().toInt())
            }
            weekViewBindings()
        }

        return binding.root
    }

    private fun weekViewBindings() {
        binding.apply {
            mondayCheckBox.addDaysOfWeekListener()
            tuesdayCheckBox.addDaysOfWeekListener()
            wednesdayCheckBox.addDaysOfWeekListener()
            thursdayCheckBox.addDaysOfWeekListener()
            fridayCheckBox.addDaysOfWeekListener()
            saturdayCheckBox.addDaysOfWeekListener()
            sundayCheckBox.addDaysOfWeekListener()
        }
    }

    fun MaterialCheckBox.addDaysOfWeekListener() {
        addOnCheckedStateChangedListener { _, _ ->
            viewModel.updateDaysOfWeek(getDaysIds())
        }
    }

    private fun unCheckAllDays() {
        binding.apply {
            mondayCheckBox.unCheck()
            tuesdayCheckBox.unCheck()
            wednesdayCheckBox.unCheck()
            thursdayCheckBox.unCheck()
            fridayCheckBox.unCheck()
            saturdayCheckBox.unCheck()
            sundayCheckBox.unCheck()
        }
    }

    fun MaterialCheckBox.unCheck() {
        isChecked = false
    }
    private fun getFrequencySelection(frequency: String?): Int {
        return when (frequency) {
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

    private fun setFrequency(frequency: String?): String {
        return when (frequency) {
            Period.DAYS.name -> stringItems[0]
            Period.WEEKS.name -> stringItems[1]
            Period.MONTHS.name -> stringItems[2]
            Period.YEARS.name -> stringItems[3]
            else -> stringItems[0]
        }
    }

    private fun getDaysIds(): List<Int>? {
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