package com.tongtongstudio.ami.ui.edit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.FragmentAddEditGoalBinding
import com.tongtongstudio.ami.receiver.ASSESSMENT_ID
import com.tongtongstudio.ami.receiver.AssessmentBroadcastReceiver
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.ui.dialog.assessment.ASSESSMENT_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.assessment.EditAssessmentDialogFragment
import com.tongtongstudio.ami.ui.dialog.assessment.NEW_USER_ASSESSMENT_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.assessment.USER_ASSESSMENT_TAG
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.*

@AndroidEntryPoint
class EditGoalFragment : Fragment(R.layout.fragment_add_edit_goal) {

    private var assessments: MutableList<Assessment> = mutableListOf()
    private val viewModel: EditGoalViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var binding: FragmentAddEditGoalBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditGoalBinding.bind(view)


        binding.apply {

            fabSaveGoal.setOnClickListener {
                saveGlobalGoal()
            }

            // assessment's goalTitle
            inputLayoutGoalTitle.editText?.setText(viewModel.goalTitle)
            inputLayoutGoalTitle.editText?.addTextChangedListener {
                if (isValidInput(it)) {
                    viewModel.goalTitle = it.toString()
                    inputLayoutGoalTitle.error = null
                } else {
                    inputLayoutGoalTitle.error = getString(R.string.error_no_title)
                    viewModel.goalTitle = ""
                }
            }

            // evaluation description
            if (viewModel.description != null)
                inputLayoutDescription.editText?.setText(viewModel.description)
            inputLayoutDescription.editText?.doOnTextChanged { text, start, before, count ->
                viewModel.description = text.toString()
            }
            // evaluation rating
            if (viewModel.goal != "null")
                inputLayoutGoal.editText?.setText(viewModel.goal)
            inputLayoutGoal.editText?.addTextChangedListener {
                if (isValidInput(it)) {
                    inputLayoutGoal.error = null
                    viewModel.goal = it.toString()
                } else {
                    binding.inputLayoutGoal.error = getString(R.string.error_goal_empty)
                    viewModel.goal = ""
                }
            }

            // evaluation unit
            if (viewModel.unit != "null" && viewModel.unit.isNotBlank())
                autocompleteTextViewUnit.setText(viewModel.unit)
            val unitOptions = arrayOf("Number", "Kg")
            val unitAdapter =
                ArrayAdapter(requireContext(), R.layout.item_options, unitOptions)
            autocompleteTextViewUnit.setAdapter(unitAdapter)
            autocompleteTextViewUnit.doOnTextChanged { text, start, before, count ->
                if (count > 0) {
                    inputLayoutUnit.error = null
                    viewModel.unit = text.toString()
                } else {
                    inputLayoutUnit.error = getString(R.string.error_unit)
                    viewModel.unit = ""
                }
            }

            // evaluation date
            if (viewModel.dueDate != null) {
                btnSetDueDate.text =
                    DateFormat.getDateInstance().format(viewModel.dueDate)
            }

            btnSetDueDate.setOnClickListener {
                val dueDatePicker =
                    showDatePickerMaterial(
                        CalendarCustomFunction.buildConstraintsForDeadline(
                            Calendar.getInstance().timeInMillis
                        ), viewModel.dueDate
                    )
                dueDatePicker.addOnPositiveButtonClickListener {
                    viewModel.dueDate = it
                    btnSetDueDate.text = DateFormat.getDateInstance().format(viewModel.dueDate)
                }
            }

            // assessment edit's section
            btnAddAssessment.setOnClickListener {
                val newFragment = EditAssessmentDialogFragment()
                newFragment.show(parentFragmentManager, USER_ASSESSMENT_TAG)
            }
            val assessmentsAdapter =
                EditAttributesAdapter<Assessment>(object : AttributeListener<Assessment> {
                    override fun onItemClicked(attribute: Assessment) {
                        //TODO: update assessment
                    }

                    override fun onRemoveCrossClick(attribute: Assessment) {
                        viewModel.removeAssessment(attribute)
                    }
                }) { binding, assessment ->
                    binding.titleOverview.text = assessment.getSumUp()
                }
            viewModel.assessments.observe(viewLifecycleOwner) {
                assessmentsAdapter.submitList(it)
                assessments = it
            }
            rvAssessments.apply {
                adapter = assessmentsAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
            }
        }

        // from edit assessment dialog
        setFragmentResultListener(NEW_USER_ASSESSMENT_REQUEST_KEY) { _, bundle ->
            val result = bundle.getParcelable<Assessment>(ASSESSMENT_RESULT_KEY)
            if (result != null) {
                viewModel.addNewAssessment(result)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.editGoalEvents.collect { event ->
                when (event) {
                    is EditGoalViewModel.EditGoalEvent.ShowInvalidInputMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                    is EditGoalViewModel.EditGoalEvent.NavigateBackWithResult -> {
                        setFragmentResult(
                            "add_edit_request",
                            bundleOf("add_edit_result" to event.result)
                        )
                        findNavController().popBackStack()
                    }
                }.exhaustive
            }
        }

    }

    private fun showDatePickerMaterial(
        constraints: CalendarConstraints,
        selection: Long? = null
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

    private fun isValidInput(input: Editable?): Boolean {
        return input.toString() != "" && input.toString() != "null"
    }

    private fun validateDueDate(): Boolean {
        return if (viewModel.dueDate == null) {
            viewModel.showInvalidInputMessage(getString(R.string.error_no_date))
            false
        } else true
    }

    private fun validateGoal(): Boolean {
        return if (viewModel.goal.isBlank()) {
            binding.inputLayoutGoal.error = getString(R.string.error_no_goal)
            false
        } else true
    }

    private fun validateTitle(): Boolean {
        return if (viewModel.goalTitle.isBlank()) {
            binding.inputLayoutGoalTitle.error = getString(R.string.error_no_title)
            false
        } else true
    }

    private fun validateUnit(): Boolean {
        return if (viewModel.unit.isBlank() || viewModel.unit == "null") {
            binding.inputLayoutUnit.error = getString(R.string.error_no_unit)
            false
        } else true
    }

    private fun saveGlobalGoal() {
        if (validateGoal() && validateTitle() && validateUnit() && validateDueDate()) {
            viewModel.saveGlobalGoal()
            for (assessment in assessments) {
                scheduleIntermediateAssessments(assessment, requireContext())
            }
        }
    }

    private fun scheduleIntermediateAssessments(assessment: Assessment, context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AssessmentBroadcastReceiver::class.java).apply {
            putExtra(ASSESSMENT_ID, assessment)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            assessment.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, assessment.dueDate, pendingIntent)
        Log.e("Alarm assessment", "Evaluation alarm set")
    }

}