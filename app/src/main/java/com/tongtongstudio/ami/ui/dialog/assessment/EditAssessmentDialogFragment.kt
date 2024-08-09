package com.tongtongstudio.ami.ui.dialog.assessment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.AssessmentType
import com.tongtongstudio.ami.databinding.DialogEditAssessmentBinding
import com.tongtongstudio.ami.util.CalendarCustomFunction
import com.tongtongstudio.ami.util.InputValidation
import java.text.DateFormat
import java.util.Calendar

const val NEW_USER_ASSESSMENT_REQUEST_KEY = "new_user_assessment_request_key"
const val ASSESSMENT_RESULT_KEY = "assessment_result_key"
const val USER_ASSESSMENT_TAG = "user_assessment_tag"
const val OBJECTIVE_ID = "objective_id"

// TODO: receive updated assessment and max due date of global targetGoal
class EditAssessmentDialogFragment : DialogFragment() {

    private lateinit var binding: DialogEditAssessmentBinding
    private lateinit var positiveButton: Button
    private var title: String? = null
    private var description: String? = null
    private var goal: Float? = null
    private var unit: String? = null
    private var dueDate: Long? = null
    private var parentObjectiveId: Long? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { fragmentActivity ->
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogEditAssessmentBinding.inflate(inflater)
            val dialogBuilder = MaterialAlertDialogBuilder(fragmentActivity)
                .setView(binding.root)
                .setTitle(getString(R.string.edit_assessment_dialog_title))
                // Add action buttons
                .setPositiveButton(R.string.ok) { dialog, wich ->
                    onDialogPositiveClick(this)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, id ->
                    getDialog()?.cancel()
                }
            val alertDialog = dialogBuilder.create()
            alertDialog.setOnShowListener {
                positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.isEnabled = false
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        /*setFragmentResultListener(NEW_USER_ASSESSMENT_REQUEST_KEY) { _, bundle ->
            val result = bundle.getLong(OBJECTIVE_ID)
            parentObjectiveId = result
        }*/

        binding.apply {
            // assessment's goalTitle
            if (title != null)
                inputLayoutTitle.editText?.setText(title)
            inputLayoutTitle.editText?.addTextChangedListener {
                if (InputValidation.isValidText(it)) {
                    title = it.toString()
                    inputLayoutTitle.error = null
                } else {
                    inputLayoutTitle.error = getString(R.string.error_no_title)
                    title = null
                }
                positiveButton.isEnabled = safeSave()
            }

            // evaluation description
            if (description != null)
                inputLayoutDescription.editText?.setText(description)
            inputLayoutDescription.editText?.doOnTextChanged { text, start, before, count ->
                description = text.toString()
            }
            // evaluation rating
            if (goal != null)
                inputLayoutGoal.editText?.setText(goal.toString())
            inputLayoutGoal.editText?.addTextChangedListener {
                if (InputValidation.isValidDecimalNum(it)) {
                    inputLayoutGoal.error = null
                    goal = it.toString().toFloat()
                } else {
                    binding.inputLayoutGoal.error = getString(R.string.error_goal_empty)
                    goal = null
                }
                positiveButton.isEnabled = safeSave()
            }

            // evaluation unit
            if (unit != null)
                autocompleteTextViewUnit.setText(unit)
            val unitOptions = resources.getStringArray(R.array.units)
            val unitAdapter =
                ArrayAdapter(requireContext(), R.layout.item_options, unitOptions)
            autocompleteTextViewUnit.setAdapter(unitAdapter)
            autocompleteTextViewUnit.doOnTextChanged { text, start, before, count ->
                if (count > 0) {
                    inputLayoutUnit.error = null
                    unit = text.toString()
                } else {
                    inputLayoutUnit.error = getString(R.string.error_unit)
                    unit = null
                }
                positiveButton.isEnabled = safeSave()
            }

            // evaluation date
            if (dueDate != null) {
                setEvaluationDate.text =
                    DateFormat.getDateInstance().format(dueDate)
            }

            setEvaluationDate.setOnClickListener {
                val dueDatePicker =
                    showDatePickerMaterial(
                        dueDate,
                        CalendarCustomFunction.buildConstraintsForDueDate(
                            Calendar.getInstance().run {
                                set(Calendar.HOUR_OF_DAY, 0)
                                timeInMillis
                            },
                            null
                        )
                    )
                dueDatePicker.addOnPositiveButtonClickListener {
                    dueDate = it
                    setEvaluationDate.text = DateFormat.getDateInstance().format(dueDate)
                    positiveButton.isEnabled = safeSave()
                }
            }
        }

        return binding.root
    }

    private fun showDatePickerMaterial(
        selection: Long?,
        constraints: CalendarConstraints
    ): MaterialDatePicker<Long> {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
                .setCalendarConstraints(constraints)
                .setSelection(selection ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()
        datePicker.show(parentFragmentManager, "datePicker")

        return datePicker
    }

    private fun safeSave(): Boolean {
        return (InputValidation.isNotNull(dueDate) && InputValidation.isNotNull(title) &&
                InputValidation.isNotNull(unit) && InputValidation.isNotNull(goal))
    }

    private fun onDialogPositiveClick(dialog: EditAssessmentDialogFragment) {
        if (!safeSave())
            return
        val assessment = Assessment(
            parentAssessmentId = parentObjectiveId,
            title = title!!,
            description = description,
            targetGoal = goal!!.toFloat(),
            unit = unit!!.toString(),
            type = AssessmentType.QUANTITY.name,
            dueDate = dueDate!!
        )
        val result = Bundle().apply {
            putParcelable(ASSESSMENT_RESULT_KEY, assessment)
        }
        dialog.setFragmentResult(
            NEW_USER_ASSESSMENT_REQUEST_KEY,
            result
        )
    }
}