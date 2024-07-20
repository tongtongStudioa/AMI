package com.tongtongstudio.ami.ui.edit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
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
import androidx.lifecycle.ViewModelProvider
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
import com.tongtongstudio.ami.adapter.AutoCompleteAdapter
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.LayoutMode
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import com.tongtongstudio.ami.data.datatables.RecurringTaskInterval
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.databinding.FragmentAddEditTaskBinding
import com.tongtongstudio.ami.receiver.*
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.ui.dialog.*
import com.tongtongstudio.ami.ui.dialog.category.CATEGORY_EDIT_TAG
import com.tongtongstudio.ami.ui.dialog.category.EditCategoryDialogFragment
import com.tongtongstudio.ami.ui.dialog.linkproject.EditProjectLinkedDialogFragment
import com.tongtongstudio.ami.ui.dialog.linkproject.PROJECT_ID
import com.tongtongstudio.ami.ui.dialog.linkproject.PROJECT_LINKED_LISTENER_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.linkproject.PROJECT_LINKED_RESULT_KEY
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class AddEditTaskFragment : Fragment(R.layout.fragment_add_edit_task) {

    private var reminders: MutableList<Reminder> = mutableListOf()
    private val viewModel: AddEditTaskViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var binding: FragmentAddEditTaskBinding

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //main view model
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        binding = FragmentAddEditTaskBinding.bind(view)

        // set toolbar with menu and navigate up icon
        setUpToolbar()

        // set view on the page
        sharedViewModel.currentLayoutMode.observe(viewLifecycleOwner) { layoutPreference ->
            setUpButtonDetails(layoutPreference.layoutMode)
            // save thing to do
            binding.fabSaveTask.setOnClickListener {
                safeSave(layoutPreference.layoutMode == LayoutMode.EXTENT)
            }
        }

        binding.apply {
            radioGroupChoiceNature.check(if (viewModel.ttdNature == Nature.TASK.name) rbTask.id else rbProject.id)
            radioGroupChoiceNature.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    rbTask.id -> viewModel.ttdNature = Nature.TASK.name
                    rbProject.id -> viewModel.ttdNature = Nature.PROJECT.name
                }
                projectSelectionGroup.isVisible = viewModel.ttdNature == Nature.TASK.name
            }

            // text view created date
            textViewCreatedDate.isVisible = viewModel.thingToDo != null
            textViewCreatedDate.text = if (viewModel.createdDateFormatted != null)
                getString(R.string.text_created_date, viewModel.createdDateFormatted) else ""

            // edit title
            editTextName.setText(viewModel.title)
            editTextName.addTextChangedListener {
                inputLayoutName.error = null
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
            editTextPriority.addTextChangedListener {
                inputLayoutPriority.error = null
                viewModel.priority = if (it.toString() == "null") "" else it.toString()
                viewModel.importance =
                    if (it.toString() == "null" || it.toString() == "") null else it.toString()
                        .toInt()
            }

            // edit category
            if (viewModel.category != null)
                autocompleteTextCategory.setText(viewModel.category!!.title)

            val adapterCategory = AutoCompleteAdapter(requireContext())
            viewModel.getCategories().observe(viewLifecycleOwner) {
                adapterCategory.submitList(it)
            }
            autocompleteTextCategory.setAdapter(adapterCategory)
            autocompleteTextCategory.setOnItemClickListener { parent, view, position, id ->
                viewModel.updateCategoryId(adapterCategory.getItem(position))
            }

            // set start date
            handleDateSelection(btnSetStartDate, removeStartDate,
                { date ->
                    validateSelectionStartDate(date)
                },
                {
                    val endDateConstraints =
                        CalendarCustomFunction.buildConstraintsForStartDate(
                            viewModel.dueDate ?: viewModel.deadline
                        )
                    showDatePickerMaterial(endDateConstraints, viewModel.dueDate)
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
                    validateSelectionDueDate(date)
                },
                {
                    val endDateConstraints = CalendarCustomFunction.buildConstraintsForDueDate(
                        viewModel.startDate ?: 0L,
                        viewModel.deadline
                    )
                    showDatePickerMaterial(endDateConstraints, viewModel.dueDate)
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
                    validateSelectionDeadline(date)
                },
                {
                    val endDateConstraints = CalendarCustomFunction.buildConstraintsForDeadline(
                        viewModel.dueDate ?: viewModel.startDate ?: 0L
                    )
                    showDatePickerMaterial(
                        endDateConstraints,
                        viewModel.deadline ?: viewModel.dueDate
                    )
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
                // TODO: add logic to make repeatable reminder until thing to do due date
                showDialogNewReminder {
                    viewModel.addNewReminder(it)
                }
            }
            val reminderAdapter =
                EditAttributesAdapter(object : AttributeListener<Reminder> {
                    override fun onItemClicked(attribute: Reminder) {
                        showDialogNewReminder(attribute.dueDate) {
                            val updatedReminder = attribute.copy(dueDate = it)
                            viewModel.updateReminder(attribute, updatedReminder)
                        }
                    }

                    override fun onRemoveCrossClick(attribute: Reminder) {
                        sharedViewModel.cancelReminder(requireContext(), attribute.id)
                        viewModel.removeReminder(attribute)
                    }
                }) { binding, reminder ->
                    binding.titleOverview.text = reminder.getReminderInformation()
                }

            viewModel.reminders.observe(viewLifecycleOwner) {
                reminderAdapter.submitList(it.toList())
                reminders = it
            }
            rvReminders.apply {
                adapter = reminderAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
            }

            // set repeatable task
            // TODO: create a custom interval for learning category tasks
            // TODO: update start date and stopAndReset on deadline
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
                            viewModel.recurringTaskInterval?.getRecurringIntervalReadable(resources),
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
                            viewModel.recurringTaskInterval?.getRecurringIntervalReadable(resources),
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
                    viewModel.recurringTaskInterval?.getRecurringIntervalReadable(resources),
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
                    TrackingTimeUtility.getFormattedEstimatedTime(viewModel.estimatedTime),
                    getString(R.string.set_estimated_time)
                )
            }

            // attach to a project
            btnAttachProject.setOnClickListener {
                // TODO: show dialog to attach, detach or change of project attached
                showDialogAttachProject()
            }
            updateSpecificButtonText(
                btnAttachProject, removeProjectLinked,
                viewModel.projectId != null,
                getString(R.string.project_linked_text, viewModel.getMainTask()),
                getString(R.string.project_linked_default)
            )
            removeProjectLinked.setOnClickListener {
                viewModel.projectId = null
                updateSpecificButtonText(
                    btnAttachProject, removeProjectLinked,
                    viewModel.projectId != null,
                    getString(R.string.project_linked_text, viewModel.getMainTask()),
                    getString(R.string.project_linked_default)
                )
            }

            // skill level and dependency
            if (viewModel.skillLevel != null)
                inputLayoutUserLevel.editText?.setText(viewModel.skillLevel.toString())
            inputLayoutUserLevel.editText?.addTextChangedListener { text ->
                viewModel.skillLevel =
                    if (text.toString() == "" || text.toString() == "null") null else text.toString()
                        .toInt()
            }

            switchDependency.isChecked = viewModel.dependency ?: false
            switchDependency.setOnCheckedChangeListener { _, isChecked ->
                viewModel.dependency = isChecked
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
                    getString(
                        R.string.estimated_time_info,
                        TrackingTimeUtility.getFormattedEstimatedTime(viewModel.estimatedTime)
                    ),
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
                result?.getRecurringIntervalReadable(resources),
                getString(R.string.repeat)
            )
        }

        // TODO: create an action with safe args with nav component
        // from dialog edit project linked
        setFragmentResultListener(PROJECT_LINKED_LISTENER_REQUEST_KEY) { _, bundle ->
            val result = bundle.getLong(PROJECT_LINKED_RESULT_KEY)
            if (result != 0L) {
                viewModel.projectId = result
            }
            updateSpecificButtonText(
                binding.btnAttachProject, binding.removeProjectLinked,
                viewModel.projectId != null,
                getString(R.string.project_linked_text, viewModel.getMainTask()),
                getString(R.string.project_linked_default)
            )
        }

        // from a project
        setFragmentResultListener("is_new_sub_task") { _, bundle ->
            val result = bundle.getLong("project_id")
            viewModel.projectId = result
            // can't change project id of add sub task demand
            binding.removeProjectLinked.isVisible = false
            binding.btnAttachProject.isClickable = false
            updateSpecificButtonText(
                binding.btnAttachProject, binding.removeProjectLinked,
                viewModel.projectId != null,
                getString(R.string.project_linked_text, viewModel.getMainTask()),
                getString(R.string.project_linked_default)
            )
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.addEditTaskEvent.collect { event ->
                when (event) {
                    is AddEditTaskViewModel.AddEditTaskEvent.ShowInvalidInputMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                    is AddEditTaskViewModel.AddEditTaskEvent.NavigateBackWithResult -> {
                        sharedViewModel.updateParentTask(viewModel.projectId)
                        clearFocus()
                        sharedViewModel.showConfirmationMessage(event.result)
                        findNavController().popBackStack()
                    }
                }.exhaustive
            }
        }
        setHasOptionsMenu(true)
    }

    private fun clearFocus() {
        binding.editTextName.clearFocus()
        binding.editTextPriority.clearFocus()
        binding.editTextDescription.clearFocus()
    }

    private fun validateSelectionDeadline(date: Long): Boolean {
        return if (!(viewModel.startDate != null && viewModel.startDate!! > date || viewModel.dueDate != null && viewModel.dueDate!! > date)) {
            true
        } else {
            Snackbar.make(
                requireView(),
                "Deadline can't be set before start date or due date",
                Snackbar.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun validateSelectionDueDate(date: Long): Boolean {
        return if (!(viewModel.startDate != null && viewModel.startDate!! > date || viewModel.deadline != null && viewModel.deadline!! < date)) {
            true
        } else {
            Snackbar.make(
                requireView(),
                "Due date can't be set after deadline or before start date",
                Snackbar.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun validateSelectionStartDate(date: Long): Boolean {
        return if (!(viewModel.dueDate != null && viewModel.dueDate!! < date || viewModel.deadline != null && viewModel.deadline!! < date)) {
            true
        } else {
            Snackbar.make(
                requireView(),
                "Start date can't be set after due date or deadline",
                Snackbar.LENGTH_SHORT
            ).show()
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_task_menu, menu)
        lifecycleScope.launch {
            val layoutMode = sharedViewModel.globalPreferencesFlow.first().layoutMode
            menu.findItem(R.id.action_update_edit_layout).isChecked =
                layoutMode == LayoutMode.SIMPLIFIED
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_new_category -> {
                showUpdateCategoryDialog()
                true
            }
            R.id.action_update_edit_layout -> {
                item.isChecked = !item.isChecked
                val layoutMode = if (item.isChecked) {
                    LayoutMode.SIMPLIFIED
                } else LayoutMode.EXTENT
                sharedViewModel.onLayoutModeSelected(layoutMode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun updateTaskRecurringStartDate(recurringTaskInterval: RecurringTaskInterval) {

        if (recurringTaskInterval.daysOfWeek == null && viewModel.dueDate == null) {
            viewModel.dueDate = Calendar.getInstance().apply {
                set(Calendar.MILLISECOND, 0)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        } else if (recurringTaskInterval.daysOfWeek != null) {
            viewModel.dueDate = recurringTaskInterval.setStartDateSpecificDay()
            viewModel.startDate = viewModel.dueDate
        }
        updateDateButtonText(
            binding.btnSetDueDate,
            viewModel.dueDate,
            binding.removeDueDate,
            getString(R.string.set_due_date)
        )
        updateDateButtonText(
            binding.btnSetStartDate,
            viewModel.startDate,
            binding.removeStartDate,
            getString(R.string.set_start_date)
        )
    }

    private fun setUpButtonDetails(layoutMode: LayoutMode) {
        val isModeExtent = layoutMode == LayoutMode.EXTENT
        binding.apply {

            updateDateButtonText(
                btnSetDueDate,
                viewModel.dueDate,
                removeDueDate,
                getString(R.string.set_due_date)
            )
            // update estimateTime button and recurring task interval button
            updateSpecificButtonText(
                btnRepeatTask, removeRepeatedChoice,
                viewModel.isRecurring,
                viewModel.recurringTaskInterval?.getRecurringIntervalReadable(resources),
                getString(R.string.repeat)
            )

            showHideViews(isModeExtent)

            // update views if mode extent
            if (isModeExtent) {
                updateDateButtonText(
                    btnSetStartDate,
                    viewModel.startDate,
                    removeStartDate,
                    getString(R.string.set_start_date)
                )
                updateDateButtonText(
                    btnSetDeadline,
                    viewModel.deadline,
                    removeDeadline,
                    getString(R.string.set_deadline)
                )
                updateSpecificButtonText(
                    btnSetEstimatedTime,
                    removeEstimatedTime,
                    viewModel.estimatedTime != null,
                    getString(
                        R.string.estimated_time_info,
                        TrackingTimeUtility.getFormattedEstimatedTime(viewModel.estimatedTime)
                    ),
                    getString(R.string.set_estimated_time)
                )
            }
        }
    }

    private fun showHideViews(modeExtent: Boolean) {
        binding.apply {
            projectSelectionGroup.isVisible = viewModel.ttdNature == Nature.TASK.name && modeExtent
            radioGroupChoiceNature.isVisible = modeExtent
            divider4.isVisible = modeExtent
            startDateSelectionGroup.isVisible = modeExtent
            deadlineSelectionGroup.isVisible = modeExtent
            estimationSelectionGroup.isVisible = modeExtent
            inputLayoutCategory.isVisible = modeExtent
            inputLayoutDescription.isVisible = modeExtent
            inputLayoutUserLevel.isVisible = modeExtent
            switchDependency.isVisible = modeExtent
            if (modeExtent) {
                editTextPriority.setText(viewModel.importance.toString().replace("null", ""))
            } else {
                editTextPriority.setText(viewModel.priority.replace("null", ""))
            }
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
                    val todayDate = Calendar.getInstance().apply {
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                    if (actionValidationDate.invoke(todayDate)) {
                        updateButtonSelection(button, getStringFromLong(todayDate), removeButton)
                        updateDataAction(todayDate)
                    }
                    true
                }
                R.id.action_tomorrow -> {
                    val tomorrowDate =
                        Calendar.getInstance().apply {
                            set(Calendar.MILLISECOND, 0)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
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
        // TODO: how to show good color on dark theme ?
        button.setIconTintResource(R.color.french_blue)
        button.setTextColor(getColor(requireContext(), R.color.french_blue))
        removeButton.isVisible = true
        button.text = valueText
    }

    private fun showEstimatedTimePicker() {
        val newFragment = EstimatedTimeDialogFragment(getString(R.string.set_estimated_time))
        newFragment.show(parentFragmentManager, ESTIMATED_TIME_DIALOG_TAG)
    }

    // TODO: 11/02/2023 delete this method to free up space
    private fun getStringFromLong(long: Long): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(long)
    }

    private fun safeSave(modeExtent: Boolean) {
        if (validateTitle() && validatePriority() && validateDueDate()) {
            viewModel.onSaveClick(modeExtent)
            for (reminder in reminders) {
                scheduleReminder(requireContext(), reminder, viewModel.title)
            }
        }
    }

    private fun scheduleReminder(context: Context, reminder: Reminder, taskName: String) {
        // TODO: record if reminder was already active
        if (reminder.dueDate < Calendar.getInstance().timeInMillis)
            return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(TASK_NAME_KEY, taskName)
            putExtra(REMINDER_DUE_DATE, reminder.dueDate)
            putExtra(REMINDER_ID, reminder.id)
            putParcelableArrayListExtra(
                REMINDER_CUSTOM_INTERVAL,
                arrayListOf(reminder.repetitionFrequency)
            )
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.dueDate, pendingIntent)
        Log.e(
            "Schedule Reminder",
            "Alarm set for: ${Date(reminder.dueDate)} and is Recurrent : ${reminder.isRecurrent}"
        )
    }


    private fun setReminderTriggerTime(date: Long, pickedHour: Int, pickedMinutes: Int): Long {
        return Calendar.getInstance().run {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, pickedHour)
            set(Calendar.MINUTE, pickedMinutes)
            timeInMillis
        }
    }

    private fun showDialogNewReminder(
        dueDateTime: Long? = null,
        actionSaveReminder: (Long) -> Unit
    ) {
        // TODO: be able to create custom interval (like repetition until due date)
        var reminderTriggerTime: Long
        // create the calendar constraint builder
        val endDateConstraints = CalendarCustomFunction.buildConstraintsForDeadline(
            Calendar.getInstance().timeInMillis
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

    private fun validateDueDate(): Boolean {
        return if (viewModel.dueDate == null) {
            viewModel.showInvalidInputMessage(getString(R.string.error_no_date))
            false
        } else true
    }

    private fun validatePriority(): Boolean {
        return if (viewModel.priority.isBlank() || viewModel.priority == "null") {
            binding.inputLayoutPriority.error = getString(R.string.error_no_priority)
            false
        } else true
    }

    private fun validateTitle(): Boolean {
        return if (viewModel.title.isBlank()) {
            binding.inputLayoutName.error = getString(R.string.error_no_title)
            false
        } else true
    }

    private fun showDialogAttachProject() {

        val bundle = bundleOf(PROJECT_ID to viewModel.projectId)
        setFragmentResult(CURRENT_RECURRING_INFO_REQUEST_KEY, bundle)
        val editProjectLinkedDialog = EditProjectLinkedDialogFragment()
        editProjectLinkedDialog.show(parentFragmentManager, "project_linked_tag")
    }

    private fun showUpdateCategoryDialog() {
        val newFragment = EditCategoryDialogFragment()
        newFragment.show(parentFragmentManager, CATEGORY_EDIT_TAG)
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
            putLong(START_DATE, if (viewModel.dueDate != null) viewModel.dueDate!! else 0L)
        }
        setFragmentResult(CURRENT_RECURRING_INFO_REQUEST_KEY, result)
        newFragment.show(parentFragmentManager, RECURRING_SELECTION_DIALOG_TAG)
    }

    private fun showDatePickerMaterial(
        constraints: CalendarConstraints,
        selection: Long? = null
    ): MaterialDatePicker<Long> {
        // clear focus of input to dismiss keyboard
        clearFocus()

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
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
                .setTitleText(getString(R.string.select_reminder_time_title))
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

