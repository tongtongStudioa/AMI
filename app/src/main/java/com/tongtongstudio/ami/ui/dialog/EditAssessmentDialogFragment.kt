package com.tongtongstudio.ami.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.DialogEditAssessmentBinding
import java.text.DateFormat

const val NEW_USER_ASSESSMENT_REQUEST_KEY = "new_user_assessment_request_key"
const val ASSESSMENT_RESULT_KEY = "assessment_result_key"
const val USER_ASSESSMENT_TAG = "user_assessment_tag"

class EditAssessmentDialogFragment : DialogFragment() {

    private lateinit var binding: DialogEditAssessmentBinding

    private var title: String? = null
    private var description: String? = null
    private var goal: Double? = null
    private var unit: String? = null
    private var dueDate: Long? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogEditAssessmentBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(binding.root)
                .setTitle("Edit an evaluation")
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

    private fun onDialogPositiveClick(dialog: EditAssessmentDialogFragment) {
        val assessment: Assessment? = safeSave()
        if (assessment != null) {
            val result = Bundle().apply {
                putParcelable(RECURRING_RESULT_KEY, assessment)
            }
            dialog.setFragmentResult(
                NEW_USER_ASSESSMENT_REQUEST_KEY,
                bundleOf(ASSESSMENT_RESULT_KEY to result)
            )
            dialog.dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.apply {

            // assessment's title
            if (title != null)
                inputLayoutTitle.editText?.setText(title)
            inputLayoutTitle.editText?.doOnTextChanged { text, _, _, _ ->
                title = text.toString()
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
                if (it.toString() != "null" && it.toString() != "")
                    goal = it.toString().toDouble()
            }

            // evaluation unit
            if (unit != null)
                autocompleteTextViewUnit.setText(unit)
            val unitOptions = arrayOf("Number", "Kg", "Minutes")
            val unitAdapter =
                ArrayAdapter(requireContext(), R.layout.list_options, unitOptions)
            autocompleteTextViewUnit.setAdapter(unitAdapter)
            autocompleteTextViewUnit.doOnTextChanged { text, start, before, count ->
                unit = text.toString()
            }

            // evaluation date
            if (dueDate != null)
                setEvaluationDate.text =
                    DateFormat.getDateInstance().format(dueDate)


            setEvaluationDate.setOnClickListener {
                val dueDatePicker =
                    showDatePickerMaterial(dueDate, CalendarConstraints.Builder().build())
                dueDatePicker.addOnPositiveButtonClickListener {
                    dueDate = it
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

    private fun safeSave(): Assessment? {
        return if (title != null && goal != null && unit != null && dueDate != null)
            Assessment(
                0,
                title = title!!,
                description = description,
                goal = goal!!.toInt(),
                unit = unit!!.toString(),
                dueDate = dueDate!!
            )
        else null
    }
}