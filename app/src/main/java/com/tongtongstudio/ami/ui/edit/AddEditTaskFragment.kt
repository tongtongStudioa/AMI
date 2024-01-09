package com.tongtongstudio.ami.ui.edit

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat.getColor
import androidx.core.os.bundleOf
import androidx.core.util.Pair
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.AttributeListener
import com.tongtongstudio.ami.adapter.EditAttributesAdapter
import com.tongtongstudio.ami.data.RecurringTaskInterval
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.databinding.AddEditTaskFragmentBinding
import com.tongtongstudio.ami.receiver.TaskNotificationManager
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.dialog.*
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class AddEditTaskFragment : Fragment(R.layout.add_edit_task_fragment) {

    private val viewModel: AddEditTaskViewModel by viewModels()
    private lateinit var taskNotificationManager: TaskNotificationManager
    private lateinit var binding: AddEditTaskFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (viewModel.thingToDo == null) taskNotificationManager =
            TaskNotificationManager(requireContext())
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setReminderTriggerTime(date: Long, pickedHour: Int, pickedMinutes: Int): Long {
        return Calendar.getInstance().run {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, pickedHour)
            set(Calendar.MINUTE, pickedMinutes)
            timeInMillis
        }
    }

    private fun showDialogNewReminder(actionSaveReminder: (Long) -> Unit) {
        var reminderTriggerTime: Long
        // create the calendar constraint builder
        val endDateConstraints = CalendarCustomFunction.buildConstraintsForDeadline(
            viewModel.startDate ?: 0L
        )
        val reminderDatePicker = showDatePickerMaterial(viewModel.deadline, endDateConstraints)

        reminderDatePicker.addOnPositiveButtonClickListener { dateInMillisSelection ->
            val timePicker = showTimePickerMaterial()
            timePicker.addOnPositiveButtonClickListener {
                val pickedHour = timePicker.hour
                val pickedMinutes = timePicker.minute
                reminderTriggerTime =
                    setReminderTriggerTime(dateInMillisSelection, pickedHour, pickedMinutes)
                actionSaveReminder(reminderTriggerTime)
            }
        }
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = AddEditTaskFragmentBinding.bind(view)

        // set toolbar with menu and navigate up icon
        setUpToolbar()

        // set view on the page
        setUpDetails()

        binding.apply {

            // text view created date
            textViewCreatedDate.isVisible = viewModel.thingToDo != null
            textViewCreatedDate.text = if (viewModel.createdDateFormatted != null)
                getString(R.string.text_created_date, viewModel.createdDateFormatted) else ""

            // edit title
            editTextName.setText(viewModel.title)
            editTextName.addTextChangedListener {
                val name = it.toString()
                viewModel.title = if (name != "") name.replaceFirst(
                    name.first(),
                    name.first().uppercaseChar()
                ) else ""
            }

            // edit description
            if (viewModel.description != null)
                inputLayoutDescription.editText?.setText(viewModel.description)
            inputLayoutDescription.editText?.doOnTextChanged { text, start, before, count ->
                viewModel.description = text.toString()
            }

            // edit priority
            editTextPriority.setText(viewModel.priority.replace("null", ""))
            editTextPriority.addTextChangedListener {
                // TODO: register mode preference
                viewModel.priority = if (it.toString() == "null") "" else it.toString()
            }

            // edit category
            // TODO: add possibility to add a new category
            if (viewModel.category != null)
                autocompleteTextCategory.setText(viewModel.category!!.title)
            val categoryOptions = mutableListOf<String>()
            categoryOptions.addAll(viewModel.getCategoriesTitle())

            val adapterCategory =
                ArrayAdapter(requireContext(), R.layout.list_options, categoryOptions)
            autocompleteTextCategory.setAdapter(adapterCategory)
            autocompleteTextCategory.addTextChangedListener {
                viewModel.updateCategoryId(it.toString())
                Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
            }
            // set start date
            handleDateSelection(btnSetStartDate, removeStartDate,
                { date ->
                    !(viewModel.dueDate != null && viewModel.dueDate!! < date || viewModel.deadline != null && viewModel.deadline!! < date)
                },
                {
                    val endDateConstraints =
                        CalendarCustomFunction.buildConstraintsForStartDate(viewModel.deadline)
                    showDatePickerMaterial(viewModel.deadline, endDateConstraints)
                },
                { newStartDate -> viewModel.startDate = newStartDate }
            )
            removeStartDate.setOnClickListener {
                viewModel.startDate = null
                viewModel.isRecurring = false
                viewModel.recurringTaskInterval = null
                updateButtonNoDataSelected(
                    btnSetStartDate,
                    removeStartDate,
                    getString(R.string.set_start_date)
                )
                updateButtonNoDataSelected(
                    btnRepeatTask,
                    removeRepeatedChoice,
                    getString(R.string.repeat)
                )
            }
            // set due date
            handleDateSelection(btnSetDueDate, removeDueDate,
                { date ->
                    !(viewModel.startDate != null && viewModel.startDate!! > date || viewModel.deadline != null && viewModel.deadline!! < date)
                },
                {
                    val endDateConstraints = CalendarCustomFunction.buildConstraintsForDueDate(
                        viewModel.startDate ?: 0L,
                        viewModel.deadline
                    )
                    showDatePickerMaterial(viewModel.deadline, endDateConstraints)
                },
                { newDueDate -> viewModel.dueDate = newDueDate }
            )
            removeDueDate.setOnClickListener {
                viewModel.dueDate = null
                updateButtonNoDataSelected(btnSetDueDate, removeDueDate, "Set due date")
            }

            // set deadline
            handleDateSelection(btnSetDeadline, removeDeadline,
                { date ->
                    !(viewModel.startDate != null && viewModel.startDate!! > date || viewModel.dueDate != null && viewModel.dueDate!! > date)
                },
                {
                    val endDateConstraints = CalendarCustomFunction.buildConstraintsForDeadline(
                        viewModel.startDate ?: 0L
                    )
                    showDatePickerMaterial(viewModel.deadline, endDateConstraints)
                },
                { newDeadline -> viewModel.deadline = newDeadline })

            removeDeadline.setOnClickListener {
                viewModel.deadline = null
                updateButtonNoDataSelected(
                    btnSetDeadline,
                    removeDeadline,
                    getString(R.string.set_deadline)
                )
            }

            // set reminder
            btnAddReminder.setOnClickListener {
                showDialogNewReminder {
                    viewModel.addNewReminder(it)
                }
            }
            val reminderAdapter =
                EditAttributesAdapter<Reminder>(object : AttributeListener<Reminder> {
                    override fun onItemClicked(attribute: Reminder) {
                        showDialogNewReminder {
                            val updatedReminder = attribute.copy(dueDate = it)
                            viewModel.updateReminder(attribute, updatedReminder)
                        }
                    }

                    override fun onRemoveCrossClick(attribute: Reminder) {
                        viewModel.removeReminder(attribute)
                    }
                })
            viewModel.reminders.observe(viewLifecycleOwner) {
                reminderAdapter.submitList(it.toList())
            }
            rvReminders.apply {
                adapter = reminderAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
            }
            // TODO: remove reminder by clicking on cross image view in the item reminder view (recycler view)

            // set repeatable task
            // TODO: create a custom interval for learning category tasks
            // TODO: update start date and stop on deadline
            val dropDownMenuRepeat = PopupMenu(requireContext(), btnRepeatTask)
            btnRepeatTask.setOnClickListener {
                dropDownMenuRepeat.show()
            }
            dropDownMenuRepeat.menuInflater.inflate(
                R.menu.popup_menu_repeatable,
                dropDownMenuRepeat.menu
            )
            dropDownMenuRepeat.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_every_day -> {
                        viewModel.isRecurring = true
                        viewModel.recurringTaskInterval = RecurringTaskInterval(1, Period.DAYS.name)
                        updateSpecificButtonText(
                            btnRepeatTask, removeRepeatedChoice,
                            viewModel.isRecurring,
                            getStringFromRecurringTaskInterval(viewModel.recurringTaskInterval),
                            getString(R.string.repeat)
                        )
                        true
                    }
                    R.id.action_every_week -> {
                        viewModel.isRecurring = true
                        viewModel.recurringTaskInterval =
                            RecurringTaskInterval(1, Period.WEEKS.name)
                        updateSpecificButtonText(
                            btnRepeatTask, removeRepeatedChoice,
                            viewModel.isRecurring,
                            getStringFromRecurringTaskInterval(viewModel.recurringTaskInterval),
                            getString(R.string.repeat)
                        )
                        true
                    }
                    R.id.action_personalized -> {
                        showRepeatableCyclePicker()
                        true
                    }
                    else -> true
                }
            }
            removeRepeatedChoice.setOnClickListener {
                viewModel.recurringTaskInterval = null
                viewModel.isRecurring = false
                updateSpecificButtonText(
                    btnRepeatTask, removeRepeatedChoice,
                    viewModel.isRecurring,
                    getStringFromRecurringTaskInterval(viewModel.recurringTaskInterval),
                    getString(R.string.repeat)
                )
            }

            // set estimated work time
            btnSetEstimatedTime.setOnClickListener {
                showEstimatedTimePicker()
            }
            removeEstimatedTime.setOnClickListener {
                viewModel.estimatedTime = null
                updateSpecificButtonText(
                    btnSetEstimatedTime, removeEstimatedTime,
                    viewModel.estimatedTime != null,
                    getStringForEstimatedTimeButton(),
                    getString(R.string.set_estimated_time)
                )
            }


            /*btnAddSubTask.setOnClickListener {
                // TODO: create an embedded dialog ?
                // todo: or drag and drop to create sub task
            }*/

            btnAddAssessment.setOnClickListener {
                val newFragment = EditAssessmentDialogFragment()
                newFragment.show(parentFragmentManager, USER_ASSESSMENT_TAG)
            }
            val assessmentsAdapter =
                EditAttributesAdapter<Assessment>(object : AttributeListener<Assessment> {
                    override fun onItemClicked(attribute: Assessment) {
                        //TODO("Not yet implemented")
                    }

                    override fun onRemoveCrossClick(attribute: Assessment) {
                        //TODO("Not yet implemented")
                    }
                })
            viewModel.assessments.observe(viewLifecycleOwner) {
                assessmentsAdapter.submitList(it)
            }
            rvAssessments.apply {
                adapter = assessmentsAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
            }

            // save thing to do
            fabSaveTask.setOnClickListener {
                safeSave()
            }
        }

        // from dialog estimated time selection
        setFragmentResultListener(ESTIMATED_TIME_LISTENER_REQUEST_KEY) { _, bundle ->
            val resultValues = bundle.getIntArray(ESTIMATED_TIME_RESULT_KEY)
            val hours: Int? = resultValues?.get(0)
            val minutes: Int? = resultValues?.get(1)
            if (hours != null && minutes != null) {
                val estimatedTimeMillis: Long =
                    (hours.toLong() * 60 * 60 * 1000) + (minutes.toLong() * 60 * 1000)
                viewModel.estimatedTime = estimatedTimeMillis
                updateSpecificButtonText(
                    binding.btnSetEstimatedTime,
                    binding.removeEstimatedTime,
                    viewModel.estimatedTime != null,
                    getStringForEstimatedTimeButton(),
                    getString(R.string.set_estimated_time)
                )
            }
        }

        // from dialog recurring selection
        setFragmentResultListener(RECURRING_REQUEST_KEY) { _, bundle ->
            val result = bundle.getParcelable<RecurringTaskInterval>(RECURRING_RESULT_KEY)
            viewModel.recurringTaskInterval = result
            viewModel.isRecurring = result != null
            // update task start date if not set
            if (result != null) {
                updateTaskRecurringStartDate(result)
            }
            updateSpecificButtonText(
                binding.btnRepeatTask,
                binding.removeRepeatedChoice,
                viewModel.isRecurring,
                getStringFromRecurringTaskInterval(result),
                getString(R.string.repeat)
            )
        }

        // from dialog assessment creation
        setFragmentResultListener(NEW_USER_ASSESSMENT_REQUEST_KEY) { _, bundle ->
            val result = bundle.getParcelable<Assessment>(ASSESSMENT_RESULT_KEY)
            if (result != null)
                viewModel.addNewAssessment(result)
        }

        // from a project
        setFragmentResultListener("is_new_sub_task") { _, bundle ->
            val result = bundle.getLong("project_id")
            viewModel.projectId = result
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.addEditTaskEvent.collect { event ->
                when (event) {
                    is AddEditTaskViewModel.AddEditTaskEvent.ShowInvalidInputMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                    is AddEditTaskViewModel.AddEditTaskEvent.NavigateBackWithResult -> {
                        binding.editTextName.clearFocus()
                        binding.editTextPriority.clearFocus()
                        binding.editTextDescription.clearFocus()
                        setFragmentResult(
                            "add_edit_request",
                            bundleOf("add_edit_result" to event.result)
                        )
                        findNavController().popBackStack()
                    }
                    /*is AddEditTaskViewModel.AddEditTaskEvent.NavigatePickerDateScreen -> {
                            val action = AddEditTaskFragmentDirections.actionGlobalDatePickerDialogFragment()
                            findNavController().navigate(action)
                        }*/
                }.exhaustive
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_task_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_new_category -> {
                // TODO: move to a new dialog
                true
            }
            R.id.action_update_edit_layout -> {
                // TODO: change layout
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateTaskRecurringStartDate(result: RecurringTaskInterval) {
        if (result.daysOfWeek == null && viewModel.startDate == null) {
            viewModel.startDate = Calendar.getInstance().timeInMillis
        } else if (result.daysOfWeek != null) {
            val startDate = Calendar.getInstance().run {
                while (get(Calendar.DAY_OF_WEEK) != result.daysOfWeek.first()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                timeInMillis
            }
            viewModel.startDate = startDate
        }
        updateDateButtonText(
            binding.btnSetStartDate,
            viewModel.startDate,
            binding.removeStartDate,
            getString(R.string.set_start_date)
        )
    }

    private fun setUpDetails() {
        binding.apply {
            updateDateButtonText(
                btnSetStartDate,
                viewModel.startDate,
                removeStartDate,
                getString(R.string.set_start_date)
            )
            updateDateButtonText(
                btnSetDueDate,
                viewModel.dueDate,
                removeDueDate,
                getString(R.string.set_due_date)
            )
            updateDateButtonText(
                btnSetDeadline,
                viewModel.deadline,
                removeDeadline,
                getString(R.string.set_deadline)
            )
            // update estimateTime button and recurring task interval button
            updateSpecificButtonText(
                btnRepeatTask, removeRepeatedChoice,
                viewModel.isRecurring,
                getStringFromRecurringTaskInterval(viewModel.recurringTaskInterval),
                getString(R.string.repeat)
            )
            updateSpecificButtonText(
                btnSetEstimatedTime, removeEstimatedTime,
                viewModel.estimatedTime != null,
                getStringForEstimatedTimeButton(),
                getString(R.string.set_estimated_time)
            )
            // TODO: maybe task reminder and assessments also
        }
    }

    private fun updateSpecificButtonText(
        button: MaterialButton,
        removeButton: View,
        valueIsSet: Boolean,
        valueText: String?,
        defaultText: String
    ) {
        if (valueIsSet && valueText != null) {
            updateButtonSelection(button, valueText, removeButton)
        } else {
            updateButtonNoDataSelected(button, removeButton, defaultText)
        }
    }

    private fun updateButtonNoDataSelected(
        button: MaterialButton,
        removeButton: View,
        defaultText: String
    ) {
        button.setIconTintResource(R.color.boulder)
        button.setTextColor(getColor(requireContext(), R.color.boulder))
        removeButton.isVisible = false
        button.text = defaultText
    }

    private fun handleDateSelection(
        button: MaterialButton,
        removeButton: View,
        actionValidationDate: (Long) -> Boolean,
        actionPersonalizedDate: () -> MaterialDatePicker<Long>,
        updateDataAction: (Long) -> Unit
    ) {
        val dropDownMenu = PopupMenu(context, button)
        button.setOnClickListener {
            dropDownMenu.show()
        }
        dropDownMenu.menuInflater.inflate(R.menu.popup_menu_date_picker, dropDownMenu.menu)
        dropDownMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_today -> {
                    val todayDate = Calendar.getInstance().timeInMillis
                    if (actionValidationDate.invoke(todayDate)) {
                        updateButtonSelection(button, getStringFromLong(todayDate), removeButton)
                        updateDataAction(todayDate)
                    }
                    true
                }
                R.id.action_tomorrow -> {
                    val tomorrowDate =
                        Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_MONTH, 1)
                        }.timeInMillis
                    if (actionValidationDate.invoke(tomorrowDate)) {
                        updateButtonSelection(button, getStringFromLong(tomorrowDate), removeButton)
                        updateDataAction(tomorrowDate)
                    }
                    true
                }
                R.id.action_personalized -> {
                    val datePicker = actionPersonalizedDate()
                    datePicker.addOnPositiveButtonClickListener { selectedDate ->
                        updateDataAction(selectedDate)
                        updateButtonSelection(button, getStringFromLong(selectedDate), removeButton)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun getStringForEstimatedTimeButton(): String {
        return if (viewModel.estimatedTime != null) {
            val hours = viewModel.estimatedTime!!.toInt() / 3600_000
            val minutes = viewModel.estimatedTime!!.toInt() / 60_000 % 60
            val minutesToString = if (minutes < 10) "0$minutes" else minutes.toString()
            getString(R.string.estimated_time_info, hours, minutesToString)
        } else ""
    }

    // TODO: change color when dark mode is activate
    private fun updateDateButtonText(
        button: MaterialButton,
        date: Long?,
        removeButton: View,
        defaultText: String
    ) {
        binding.apply {
            if (date != null) {
                updateButtonSelection(button, getStringFromLong(date), removeButton)
            } else {
                updateButtonNoDataSelected(button, removeButton, defaultText)
            }
        }
    }

    private fun updateButtonSelection(
        button: MaterialButton,
        valueText: String,
        removeButton: View
    ) {
        button.setIconTintResource(R.color.french_blue)
        button.setTextColor(getColor(requireContext(), R.color.french_blue))
        removeButton.isVisible = true
        button.text = valueText
    }

    private fun showEstimatedTimePicker() {
        val newFragment = EstimatedTimeDialogFragment()
        newFragment.show(parentFragmentManager, ESTIMATED_TIME_DIALOG_TAG)
    }

    // TODO: 11/02/2023 delete this method to free up space
    private fun getStringFromLong(long: Long): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(long)
    }

    private fun safeSave() {
        if (viewModel.title.isBlank()) {
            //binding.inputLayoutName.error = "Name !"
            viewModel.showInvalidInputMessage(getString(R.string.error_no_name))
            return
        } else if (viewModel.priority.isBlank() || viewModel.priority == "null") {
            viewModel.showInvalidInputMessage(getString(R.string.error_no_priority))
            return
        } else if (viewModel.dueDate == null) {
            viewModel.showInvalidInputMessage(getString(R.string.error_no_date))
            return
        } /*else if (viewModel.deadline == null && viewModel.isRecurring) {
            viewModel.showInvalidInputMessage("Pick a deadline for recurring end")
        }*/
        //scheduleReminder(viewModel.reminder)
        viewModel.onSaveClick()
    }

    private fun showRepeatableCyclePicker() {
        val newFragment = RecurringChoiceDialogFragment()
        val result = Bundle().apply {
            putInt(TIMES_KEY, viewModel.recurringTaskInterval?.times ?: 0)
            putString(PERIOD_KEY, viewModel.recurringTaskInterval?.period ?: NO_VALUE)
            putIntArray(
                DAYS_OF_THE_WEEKS_KEY,
                viewModel.recurringTaskInterval?.daysOfWeek?.toIntArray() ?: IntArray(1)
            )
            putString(
                DEADLINE,
                if (viewModel.deadline != null) getStringFromLong(viewModel.deadline!!) else NO_VALUE
            )
            putLong(START_DATE, if (viewModel.startDate != null) viewModel.startDate!! else 0L)
        }
        setFragmentResult(CURRENT_RECURRING_INFO_REQUEST_KEY, result)
        newFragment.show(parentFragmentManager, RECURRING_SELECTION_DIALOG_TAG)
    }

    private fun getStringFromRecurringTaskInterval(recurringTaskInterval: RecurringTaskInterval?): String {
        return if (recurringTaskInterval != null) {
            if (recurringTaskInterval.times == 1 && recurringTaskInterval.daysOfWeek == null) {
                when (recurringTaskInterval.period) {
                    Period.DAYS.name -> getString(R.string.each_days)
                    Period.WEEKS.name -> getString(R.string.each_weeks)
                    Period.MONTHS.name -> getString(R.string.each_months)
                    Period.YEARS.name -> getString(R.string.each_years)
                    else -> getString(R.string.each_days)
                }
            } else if (recurringTaskInterval.daysOfWeek != null) { // TODO: change this to best integration
                if (recurringTaskInterval.times == 1) {
                    "Weekly on " + recurringTaskInterval.daysOfWeek

                } else "On " + recurringTaskInterval.daysOfWeek + " every " + recurringTaskInterval.times + " weeks"
            } else {
                when (recurringTaskInterval.period) {
                    Period.DAYS.name -> getString(
                        R.string.every_x_days,
                        recurringTaskInterval.times
                    )
                    Period.WEEKS.name -> getString(
                        R.string.every_x_weeks,
                        recurringTaskInterval.times
                    )
                    Period.MONTHS.name -> getString(
                        R.string.every_x_months,
                        recurringTaskInterval.times
                    )
                    Period.YEARS.name -> getString(
                        R.string.every_x_years,
                        recurringTaskInterval.times
                    )
                    else -> getString(R.string.every_x_days, recurringTaskInterval.times)
                }
            }
        } else ""
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

    private fun showTimePickerMaterial(): MaterialTimePicker {
        val timePicker =
            MaterialTimePicker.Builder()
                .setTitleText("Select Time Reminder")
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(9)
                .setMinute(30)
                .setInputMode(INPUT_MODE_CLOCK)
                .build()
        timePicker.show(parentFragmentManager, "timePicker")

        return timePicker
    }

    private fun showDateRangePickerMaterial(): MaterialDatePicker<Pair<Long, Long>> {
        val dateRangePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.Select_two_date_to_spread_event))
                .build()
        dateRangePicker.show(parentFragmentManager, "dateRangePicker")

        return dateRangePicker
    }

    // TODO: find best way to display that toolbar
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
        binding.toolbar.setNavigationOnClickListener {
            navController.navigateUp(appBarConfiguration)
        }
    }
}

