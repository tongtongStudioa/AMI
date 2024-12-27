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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
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
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.AutoCompleteAdapter
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.LayoutMode
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import com.tongtongstudio.ami.data.datatables.RecurringTaskInterval
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.TaskRecurrence
import com.tongtongstudio.ami.data.datatables.TaskRecurrenceWithDays
import com.tongtongstudio.ami.databinding.FragmentAddEditTaskBinding
import com.tongtongstudio.ami.receiver.REMINDER_CUSTOM_INTERVAL
import com.tongtongstudio.ami.receiver.REMINDER_DUE_DATE
import com.tongtongstudio.ami.receiver.REMINDER_ID
import com.tongtongstudio.ami.receiver.ReminderBroadcastReceiver
import com.tongtongstudio.ami.receiver.TASK_NAME_KEY
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.ui.dialog.CURRENT_RECURRING_INFO_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.DAYS_OF_THE_WEEKS_KEY
import com.tongtongstudio.ami.ui.dialog.DEADLINE
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_DIALOG_TAG
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_LISTENER_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.NO_VALUE
import com.tongtongstudio.ami.ui.dialog.PERIOD_KEY
import com.tongtongstudio.ami.ui.dialog.Period
import com.tongtongstudio.ami.ui.dialog.RECURRING_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.RECURRING_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.RECURRING_SELECTION_DIALOG_TAG
import com.tongtongstudio.ami.ui.dialog.RecurringChoiceDialogFragment
import com.tongtongstudio.ami.ui.dialog.START_DATE
import com.tongtongstudio.ami.ui.dialog.TIMES_KEY
import com.tongtongstudio.ami.ui.dialog.TimePickerDialogFragment
import com.tongtongstudio.ami.ui.dialog.category.CATEGORY_EDIT_TAG
import com.tongtongstudio.ami.ui.dialog.category.EditCategoryDialogFragment
import com.tongtongstudio.ami.ui.dialog.linkproject.EditProjectLinkedDialogFragment
import com.tongtongstudio.ami.ui.dialog.linkproject.PROJECT_ID
import com.tongtongstudio.ami.ui.dialog.linkproject.PROJECT_LINKED_LISTENER_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.linkproject.PROJECT_LINKED_RESULT_KEY
import com.tongtongstudio.ami.util.CalendarCustomFunction
import com.tongtongstudio.ami.util.DateTimePicker
import com.tongtongstudio.ami.util.InputValidation
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@AndroidEntryPoint
class AddEditTaskFragment : Fragment(R.layout.fragment_add_edit_task) {

    private var reminders: MutableList<Reminder> = mutableListOf()
    private val viewModel: AddEditTaskViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var binding: FragmentAddEditTaskBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var dateTimePicker: DateTimePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //main view model
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // set dateTimePicker object
        dateTimePicker = DateTimePicker(parentFragmentManager, requireContext())

        // Initialize the permission launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
                dateTimePicker.showDialogNewReminder {
                    viewModel.addNewReminder(it)
                }
            } else showPermissionRationale()
        }
    }
    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        // Transition d'entrée avec MaterialSharedAxis
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }

        /*binding.nestedScroolView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                // Défilement vers le bas
                binding.fabSaveTask.hide() // Cacher le FAB
            } else {
                // Défilement vers le haut
                binding.fabSaveTask.show() // Réafficher le FAB
            }
        }*/

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
                if (InputValidation.isValidText(it)) {
                    inputLayoutName.error = null
                    val name = it.toString()
                    viewModel.title = name.replaceFirst(
                        name.first(),
                        name.first().uppercaseChar()
                    )
                } else {
                    viewModel.title = ""
                    inputLayoutName.error = getString(R.string.error_no_title)
                }
            }

            // edit description
            if (viewModel.description != null)
                inputLayoutDescription.editText?.setText(viewModel.description)
            inputLayoutDescription.editText?.doOnTextChanged { text, start, before, count ->
                viewModel.description = text.toString()
            }

            // edit priority
            if (InputValidation.isValidPriority(viewModel.priority)) {
                val text = if (viewModel.priority != null) viewModel.priority.toString() else ""
                editTextPriority.setText(text)
            }
            editTextPriority.addTextChangedListener {
                if (InputValidation.isValidPriority(it)) {
                    inputLayoutPriority.error = null
                    viewModel.priority = it.toString().toInt()
                    viewModel.importance = it.toString().toInt()
                } else {
                    inputLayoutPriority.error = getString(R.string.error_no_priority)
                    viewModel.priority = null
                    viewModel.importance = null
                }
            }

            // edit category
            viewModel.category.observe(viewLifecycleOwner) {
                if (it != null) {
                    autocompleteTextCategory.setText(it.title)
                } else autocompleteTextCategory.setText("")
            }
            val adapterCategory = AutoCompleteAdapter(requireContext())
            viewModel.getCategories().observe(viewLifecycleOwner) {
                if (!it.contains(viewModel.category.value)) {
                    viewModel.updateCategory(null)
                }
                adapterCategory.submitList(it)
            }
            autocompleteTextCategory.setAdapter(adapterCategory)
            autocompleteTextCategory.setOnItemClickListener { parent, view, position, id ->
                viewModel.updateCategory(adapterCategory.getCategorySelected(position))
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
                    dateTimePicker.showDatePickerMaterial(endDateConstraints, viewModel.dueDate)
                },
                { newStartDate -> viewModel.startDate = newStartDate }
            )
            removeStartDate.setOnClickListener {
                viewModel.startDate = null
                viewModel.taskRecurrenceWithDays = null
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
                    dateTimePicker.showDatePickerMaterial(endDateConstraints, viewModel.dueDate)
                },
                { newDueDate -> viewModel.dueDate = newDueDate }
            )
            removeDueDate.setOnClickListener {
                viewModel.dueDate = null
                updateButtonNoDataSelected(
                    btnSetDueDate,
                    removeDueDate,
                    getString(R.string.set_due_date)
                )
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
                    dateTimePicker.showDatePickerMaterial(
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
                if (isNotificationPermissionGranted())
                    dateTimePicker.showDialogNewReminder {
                        viewModel.addNewReminder(it)
                    }
                else requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            val reminderAdapter =
                EditAttributesAdapter(object : AttributeListener<Reminder> {
                    override fun onItemClicked(attribute: Reminder) {
                        if (isNotificationPermissionGranted())
                            dateTimePicker.showDialogNewReminder(attribute.dueDate) {
                                val updatedReminder = attribute.copy(dueDate = it)
                                viewModel.updateReminder(attribute, updatedReminder)
                            }
                        else requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    override fun onRemoveCrossClick(attribute: Reminder) {
                        sharedViewModel.cancelReminder(requireContext(), attribute.id)
                        viewModel.removeReminder(attribute)
                    }
                }) { binding, reminder ->
                    binding.titleOverview.text = getString(
                        R.string.reminder_informtions_overview,
                        reminder.getDueDateFormatted(),
                        reminder.getTimeFormatted()
                    )
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
                        viewModel.taskRecurrence =
                            TaskRecurrence(Period.DAYS.name,1,viewModel.startDate)
                        updateSpecificButtonText(
                            btnRepeatTask, removeRepeatedChoice,
                            viewModel.taskRecurrenceWithDays != null,
                            viewModel.taskRecurrenceWithDays?.getRecurringIntervalReadable(resources),
                            getString(R.string.repeat)
                        )
                        true
                    }

                    R.id.action_every_week -> {
                        viewModel.taskRecurrence =
                            TaskRecurrence(Period.WEEKS.name,1,viewModel.startDate)
                        updateSpecificButtonText(
                            btnRepeatTask, removeRepeatedChoice,
                            viewModel.taskRecurrenceWithDays != null,
                            viewModel.taskRecurrenceWithDays?.getRecurringIntervalReadable(resources),
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
                viewModel.taskRecurrenceWithDays = null
                updateSpecificButtonText(
                    btnRepeatTask, removeRepeatedChoice,
                    viewModel.taskRecurrenceWithDays != null,
                    viewModel.taskRecurrenceWithDays?.getRecurringIntervalReadable(resources),
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

            // TODO: change add add logic to update dependency on other task
            // TODO: add view for dependency task
        }

        // from dialog estimated time selection
        setFragmentResultListener(ESTIMATED_TIME_LISTENER_REQUEST_KEY) { _, bundle ->
            val result = bundle.getLong(ESTIMATED_TIME_RESULT_KEY)
            if (result != 0L) {
                viewModel.estimatedTime = result
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

        // TODO: change recurring choice dialog return object
        // from dialog recurring selection
        setFragmentResultListener(RECURRING_REQUEST_KEY) { _, bundle ->
            val result = bundle.getParcelable<TaskRecurrenceWithDays>(RECURRING_RESULT_KEY)
            viewModel.taskRecurrenceWithDays = result
            // update task start date if not set
            if (result != null) {
                updateTaskRecurringStartDate(result)
            }
            updateSpecificButtonText(
                binding.btnRepeatTask,
                binding.removeRepeatedChoice,
                viewModel.taskRecurrenceWithDays != null,
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
                        // update project if task is linked
                        sharedViewModel.updateParentTask(viewModel.projectId)
                        clearFocus()
                        sharedViewModel.showConfirmationMessage(event.result)
                        findNavController().popBackStack()
                    }
                }.exhaustive
            }
        }
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.edit_task_menu, menu)
                lifecycleScope.launch {
                    val layoutMode = sharedViewModel.globalPreferencesFlow.first().layoutMode
                    menu.findItem(R.id.action_update_edit_layout).isChecked =
                        layoutMode == LayoutMode.SIMPLIFIED
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_create_new_category -> {
                        showUpdateCategoryDialog()
                        true
                    }

                    R.id.action_update_edit_layout -> {
                        menuItem.isChecked = !menuItem.isChecked
                        val layoutMode = if (menuItem.isChecked) {
                            LayoutMode.SIMPLIFIED
                        } else LayoutMode.EXTENT
                        sharedViewModel.onLayoutModeSelected(layoutMode)
                        true
                    }

                    else -> false
                }
            }

        }, viewLifecycleOwner)
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
                getString(R.string.msg_invalid_deadline),
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
                getString(R.string.msg_invalid_due_date),
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
                getString(R.string.msg_invalid_start_date),
                Snackbar.LENGTH_SHORT
            ).show()
            false
        }
    }


    private fun updateTaskRecurringStartDate(taskRecurrenceWithDays: TaskRecurrenceWithDays?) {

        if (taskRecurrenceWithDays?.daysOfWeek == null && viewModel.dueDate == null) {
            viewModel.dueDate = Calendar.getInstance().apply {
                set(Calendar.MILLISECOND, 0)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        } else if (taskRecurrenceWithDays?.daysOfWeek != null) {
            viewModel.dueDate = taskRecurrenceWithDays.setStartDateSpecificDay()
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
                viewModel.taskRecurrenceWithDays != null,
                viewModel.taskRecurrenceWithDays?.getRecurringIntervalReadable(resources),
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
            /*if (modeExtent) {
                editTextPriority.setText(viewModel.importance.toString().replace("null", ""))
            } else {
                editTextPriority.setText(viewModel.priority.replace("null", ""))
            }*/
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
        //button.setIconTintResource(R.color.md_theme_light_inversePrimary)
        button.setTextColor(MaterialColors.getColor(requireView(), R.attr.colorPrimaryInverse))
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
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
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
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
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
                    datePicker.addOnPositiveButtonClickListener { date ->
                        val selectedDate = Calendar.getInstance().apply {
                            timeInMillis = date
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }.timeInMillis
                        updateDataAction(selectedDate)
                        updateButtonSelection(button, getStringFromLong(selectedDate), removeButton)
                    }
                    true
                }

                else -> false
            }
        }
    }

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
        button.setTextColor(MaterialColors.getColor(requireView(), R.attr.colorPrimary))
        removeButton.isVisible = true
        button.text = valueText
    }

    private fun showEstimatedTimePicker() {
        val newFragment = TimePickerDialogFragment(getString(R.string.set_estimated_time))
        newFragment.show(parentFragmentManager, ESTIMATED_TIME_DIALOG_TAG)
    }

    private fun getStringFromLong(long: Long): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(long)
    }

    private fun safeSave(modeExtent: Boolean) {
        /*if (!InputValidation.isNotNull(viewModel.dueDate)) {
            viewModel.showInvalidInputMessage(getString(R.string.error_no_date))
            return
        }*/
        /*if (InputValidation.isValidText(viewModel.title) && InputValidation.isValidText(viewModel.priority) && InputValidation.isNotNull(viewModel.dueDate))
            viewModel.showInvalidInputMessage("Draft task")*/
        if (InputValidation.isValidText(viewModel.title)) {
            viewModel.onSaveClick(modeExtent)
            for (reminder in reminders) {
                scheduleReminder(requireContext(), reminder, viewModel.title)
            }
        } else binding.inputLayoutName.error = getString(R.string.error_no_title)
    }

    private fun scheduleReminder(context: Context, reminder: Reminder, taskName: String) {
        if (reminder.isPassed())
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
        /*Log.e(
            "Schedule Reminder",
            "Alarm set for: ${Date(reminder.dueDate)} and is Recurrent : ${reminder.isRecurrent}"
        )*/
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

    // TODO: update this method and change to navigation component
    private fun showRepeatableCyclePicker() {
        val newFragment = RecurringChoiceDialogFragment()
        val result = Bundle().apply {
            putInt(TIMES_KEY, viewModel.taskRecurrenceWithDays?.taskRecurrence?.interval ?: 0)
            putString(PERIOD_KEY, viewModel.taskRecurrenceWithDays?.taskRecurrence?.frequency ?: NO_VALUE)
            /*putParcelableArrayList(
                DAYS_OF_THE_WEEKS_KEY,
                viewModel.taskRecurrenceWithDays?.daysOfWeek
            )*/
            putString(
                DEADLINE,
                if (viewModel.deadline != null) getStringFromLong(viewModel.deadline!!) else NO_VALUE
            )
            putLong(START_DATE, if (viewModel.dueDate != null) viewModel.dueDate!! else 0L)
        }
        setFragmentResult(CURRENT_RECURRING_INFO_REQUEST_KEY, result)
        newFragment.show(parentFragmentManager, RECURRING_SELECTION_DIALOG_TAG)
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
        binding.collapseToolbar.setupWithNavController(
            binding.toolbar,
            navController,
            appBarConfiguration
        )
        binding.toolbar.setNavigationOnClickListener {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = resources.getInteger(R.integer.middle_duration).toLong()
            }
            navController.navigateUp(appBarConfiguration)
        }
    }
}

