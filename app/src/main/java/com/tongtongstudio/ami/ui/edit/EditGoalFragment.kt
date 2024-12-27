package com.tongtongstudio.ami.ui.edit

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.FragmentAddEditGoalBinding
import com.tongtongstudio.ami.receiver.ASSESSMENT_ID
import com.tongtongstudio.ami.receiver.AssessmentBroadcastReceiver
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.dialog.assessment.ASSESSMENT_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.assessment.EditAssessmentDialogFragment
import com.tongtongstudio.ami.ui.dialog.assessment.NEW_USER_ASSESSMENT_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.assessment.USER_ASSESSMENT_TAG
import com.tongtongstudio.ami.util.CalendarCustomFunction
import com.tongtongstudio.ami.util.InputValidation
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar

@AndroidEntryPoint
class EditGoalFragment : Fragment(R.layout.fragment_add_edit_goal) {

    private var assessments: MutableList<Assessment> = mutableListOf()
    private val viewModel: EditGoalViewModel by viewModels()
    private lateinit var binding: FragmentAddEditGoalBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the permission launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) showPermissionRationale()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditGoalBinding.bind(view)
        setUpToolbar()

        // Transition d'entrée avec MaterialSharedAxis
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }

        binding.apply {

            fabSaveGoal.setOnClickListener {
                saveGlobalGoal()
            }

            // assessment's goalTitle
            inputLayoutGoalTitle.editText?.setText(viewModel.goalTitle)
            inputLayoutGoalTitle.editText?.addTextChangedListener {
                if (InputValidation.isValidText(it)) {
                    val name = it.toString()
                    viewModel.goalTitle = if (name != "") name.replaceFirst(
                        name.first(),
                        name.first().uppercaseChar()
                    ) else ""
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

            // evaluation type 
            radioGroupUnitType.setOnCheckedChangeListener { radioGroup, i ->
                when (radioGroup.checkedRadioButtonId) {
                    // TODO: adapt targetScore type
                    rbQuantity.id -> {
                        // TODO: remove unit and show "without unit" 
                    }

                    rbDuration.id -> {
                        // TODO: adapt targetScore type
                    }

                    rbBoolean.id -> {
                        // TODO: adapt targetScore type
                    }
                }
            }

            // evaluation target figure
            if (viewModel.goal != "null")
                inputLayoutGoal.editText?.setText(viewModel.goal)
            inputLayoutGoal.editText?.addTextChangedListener {
                if (InputValidation.isValidDecimalNum(it)) {
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
            val unitOptions = resources.getStringArray(R.array.units)
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
            } else {
                removeDueDate.isVisible = false
                btnSetDueDate.setTextColor(
                    MaterialColors.getColor(
                        requireView(),
                        R.attr.colorPrimaryInverse
                    )
                )
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
                    removeDueDate.isVisible = true
                    btnSetDueDate.setTextColor(
                        MaterialColors.getColor(
                            requireView(),
                            R.attr.colorPrimary
                        )
                    )
                }
            }
            removeDueDate.setOnClickListener {
                btnSetDueDate.setTextColor(
                    MaterialColors.getColor(
                        requireView(),
                        R.attr.colorPrimaryInverse
                    )
                )
                removeDueDate.isVisible = false
                viewModel.dueDate = null
                btnSetDueDate.text = getString(R.string.set_due_date)
            }

            // assessment edit's section
            btnAddAssessment.setOnClickListener {
                if (isNotificationPermissionGranted())
                    showIntermediateAssessment()
                else requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            val assessmentsAdapter =
                EditAttributesAdapter(object : AttributeListener<Assessment> {
                    override fun onItemClicked(attribute: Assessment) {
                        // TODO: add showAssessmentDialog function
                    }

                    override fun onRemoveCrossClick(attribute: Assessment) {
                        viewModel.removeAssessment(attribute)
                    }
                }) { binding, assessment ->
                    binding.titleOverview.text = getString(
                        R.string.assessment_informations_overview,
                        assessment.title,
                        assessment.getFormattedDueDate()
                    )
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

        // TODO: edit this to update assessment
        // from edit assessment dialog
        setFragmentResultListener(NEW_USER_ASSESSMENT_REQUEST_KEY) { _, bundle ->
            val result = bundle.getParcelable<Assessment>(ASSESSMENT_RESULT_KEY)
            if (result != null) {
                viewModel.addNewAssessment(result)
            }
        }

        // respond to event
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
                            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                                interpolator = AccelerateDecelerateInterpolator()
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            findNavController().popBackStack()
                        }
                    }.exhaustive
                }
            }
        }
    }

    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    true
                }

                else -> {
                    // Request permission
                    false
                }
            }
        } else {
            // For devices running on versions below Android 13
            true
        }
    }

    private fun showIntermediateAssessment() {
        val newFragment = EditAssessmentDialogFragment()
        newFragment.show(parentFragmentManager, USER_ASSESSMENT_TAG)
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

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.permission_needed))
            .setMessage(getString(R.string.this_app_requires_notification_permission_to_send_you_reminders))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", activity?.packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun saveGlobalGoal() {
        if (!InputValidation.isNotNull(viewModel.dueDate)) {
            viewModel.showInvalidInputMessage(getString(R.string.error_no_date))
            return
        }
        if (!(InputValidation.isValidText(viewModel.goal) &&
                    InputValidation.isValidText(viewModel.goalTitle) &&
                    InputValidation.isValidText(viewModel.unit))
        )
            return

        viewModel.saveGlobalGoal()
        for (assessment in assessments) {
            scheduleIntermediateAssessments(assessment, requireContext())
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
    }

    // function to set up toolbar with collapse toolbar and link to drawer layout
    private fun setUpToolbar() {
        val mainActivity = activity as MainActivity
        // imperative to see option menu and navigation icon (hamburger)
        mainActivity.setSupportActionBar(binding.toolbar)

        val navController = findNavController()
        // retrieve app bar configuration : see MainActivity.class
        val appBarConfiguration = mainActivity.appBarConfiguration

        // to set hamburger menu work and open drawer layout
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
    }

}