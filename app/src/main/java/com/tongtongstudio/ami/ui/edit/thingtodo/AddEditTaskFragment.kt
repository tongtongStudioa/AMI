package com.tongtongstudio.ami.ui.edit.thingtodo

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
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.AutoCompleteAdapter
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.LayoutMode
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Task
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
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_DIALOG_TAG
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_LISTENER_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.TimePickerDialogFragment
import com.tongtongstudio.ami.ui.dialog.blocking_task.BLOCKING_TASK_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.blocking_task.BLOCKING_TASK_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.category.CATEGORY_EDIT_TAG
import com.tongtongstudio.ami.ui.dialog.category.EditCategoryDialogFragment
import com.tongtongstudio.ami.ui.dialog.parent_project.PROJECT_LINKED_LISTENER_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.parent_project.PROJECT_LINKED_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.recurring_task.CURRENT_RECURRING_INFO_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.recurring_task.EditRecurringDialogFragment
import com.tongtongstudio.ami.ui.dialog.recurring_task.Period
import com.tongtongstudio.ami.ui.dialog.recurring_task.RECURRING_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.recurring_task.RECURRING_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.recurring_task.RECURRING_SELECTION_DIALOG_TAG
import com.tongtongstudio.ami.util.CalendarCustomFunction
import com.tongtongstudio.ami.util.DateTimePicker
import com.tongtongstudio.ami.util.InputValidation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@AndroidEntryPoint
class AddEditTaskFragment : Fragment(R.layout.fragment_add_edit_task) {

    private val viewModel: AddEditTaskViewModel by viewModels()
    private val sharedViewModel: MainViewModel by viewModels()
    private lateinit var binding: FragmentAddEditTaskBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var reminderAdapter: EditAttributesAdapter<Reminder>
    private lateinit var dateTimePicker: DateTimePicker
    private lateinit var categoryAdapter: AutoCompleteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set dateTimePicker object
        dateTimePicker = DateTimePicker(parentFragmentManager, requireContext())

        // Initialize the permission launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
                dateTimePicker.createReminderDialog {
                    val reminder = it
                    viewModel.addReminder(reminder)
                }
            } else showPermissionRationale()
        }
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
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
        /*binding.nestedScroolView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                // Défilement vers le bas
                binding.fabSaveTask.hide() // Cacher le FAB
            } else {
                // Défilement vers le haut
                binding.fabSaveTask.show() // Réafficher le FAB
            }
        }*/

        // Pour les éléments non-editables
        viewModel.uiState.onEach()
        { state ->
            binding.apply {
                // Creation date task's
                textViewCreatedDate.isVisible = state.thingToDo != null
                textViewCreatedDate.text =
                    getString(R.string.text_created_date, state.creationDateFormatted)
                //progressBar.isVisible = state.isLoading
            }
        }.launchIn(lifecycleScope)

        binding.apply {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.uiState
                    .filter { it.title.isNotBlank() && it.thingToDo != null }
                    .take(1)
                    .collect {
                        editTextName.setText(it.title)
                        editTextDescription.setText(it.description)
                        editTextPriority.setText(it.priority?.toString() ?: "")
                        editTextUserLevel.setText(it.skillLevel?.toString() ?: "")
                        val radBtnChecked =
                            if (it.nature == Nature.TASK.name || it.nature == Nature.SUB_TASK.name) rbTask.id else rbProject.id
                        radioGroupChoiceNature.check(radBtnChecked)
                        autocompleteTextCategory.setText(it.category?.title ?: "")
                    }
            }

            autocompleteTextCategory.setOnItemClickListener { parent, view, position, id ->
                val categorySelected = categoryAdapter.getCategorySelected(position)
                if (categorySelected.id == 0L) {
                    viewModel.updateCategory(null)
                    autocompleteTextCategory.setText("")
                } else {
                    viewModel.updateCategory(categorySelected)
                }
            }
        }

        setupAdapters()
        setupUiBindings()
        setupObservers()

        // from dialog estimated time selection
        setFragmentResultListener(ESTIMATED_TIME_LISTENER_REQUEST_KEY) { _, bundle ->
            val result = bundle.getLong(ESTIMATED_TIME_RESULT_KEY)
            if (result != 0L) {
                viewModel.updateEstimatedWorkTime(result)
            }
        }

        // from dialog recurring selection
        setFragmentResultListener(RECURRING_REQUEST_KEY) { _, bundle ->
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(RECURRING_RESULT_KEY, TaskRecurrenceWithDays::class.java)
            } else {
                bundle.getParcelable(RECURRING_RESULT_KEY)
            }
            viewModel.updateRecurrenceInfos(result)
            // TODO: update start date by default if no start date is set
        }

        // from dialog edit parent project
        setFragmentResultListener(PROJECT_LINKED_LISTENER_REQUEST_KEY) { _, bundle ->
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(PROJECT_LINKED_RESULT_KEY, Task::class.java)
            } else {
                bundle.getParcelable(PROJECT_LINKED_RESULT_KEY)
            }
            viewModel.updateParentProject(result)
        }

        // from dialog edit blocking task
        setFragmentResultListener(BLOCKING_TASK_REQUEST_KEY) { _, bundle ->
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(BLOCKING_TASK_RESULT_KEY, Task::class.java)
            } else {
                bundle.getParcelable(BLOCKING_TASK_RESULT_KEY)
            }
            viewModel.updateBlockingTask(result)
        }

        // from a project
        setFragmentResultListener("is_new_sub_task") { _, bundle ->
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable("project", Task::class.java)
            } else {
                bundle.getParcelable("project")
            }
            viewModel.updateParentProject(result)
            // can't change project id of add sub task demand
            binding.removeProjectLinked.isVisible = false
            binding.btnAttachProject.isClickable = false
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
                        showCategoryDialog()
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


    private fun setupAdapters() {
        reminderAdapter =
            EditAttributesAdapter(object : AttributeListener<Reminder> {
                override fun onItemClicked(attribute: Reminder) {
                    if (isNotificationPermissionGranted())
                        dateTimePicker.createReminderDialog(attribute) {
                            viewModel.updateReminder(it)
                        }
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .map { it.reminders }
                .distinctUntilChanged()
                .collect { reminders ->
                    reminderAdapter.submitList(reminders)
                }
        }

        binding.rvReminders.apply {
            adapter = reminderAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }

        categoryAdapter = AutoCompleteAdapter(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .map { it.categorySuggestions.toMutableList() }
                .collect { suggestions ->
                    if (!suggestions.contains(viewModel.uiState.value.category)) {
                        viewModel.updateCategory(null)
                    }
                    if (viewModel.uiState.value.category != null) {
                        suggestions.add(0, Category(getString(R.string.no_category), null))
                    }
                    categoryAdapter.submitList(suggestions)
                }
        }
        binding.autocompleteTextCategory.setAdapter(categoryAdapter)
    }

    private fun setupUiBindings() {
        binding.apply {
            // edit title
            /*lifecycleScope.launch {
                viewModel.isFormValid.collect { isValid ->
                    fabSaveTask.isEnabled = isValid
                    if (!isValid) inputLayoutName.error = getString(R.string.error_no_title)
                }
            }*/
            radioGroupChoiceNature.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    rbTask.id -> viewModel.updateNature(Nature.TASK.name)
                    rbProject.id -> viewModel.updateNature(Nature.PROJECT.name)
                }
            }

            editTextName.bindTextTo { text ->
                if (InputValidation.isValidText(text)) {
                    inputLayoutName.error = null
                    viewModel.updateTitle(text.replaceFirstChar { char ->
                        char.uppercaseChar()
                    })
                } else {
                    inputLayoutName.error = getString(R.string.error_no_title)
                    viewModel.updateTitle("")
                }
            }

            // edit Priority
            editTextPriority.bindTextTo { text ->
                if (InputValidation.isValidPriority(text)) {
                    viewModel.updatePriority(text.toInt())
                }
            }

            inputLayoutDescription.editText?.bindTextTo { viewModel.updateDescription(it) }

            // set reminder
            btnAddReminder.setOnClickListener {
                // TODO: add logic to make repeatable reminder until thing to do due date
                if (isNotificationPermissionGranted())
                    dateTimePicker.createReminderDialog {
                        viewModel.addReminder(it)
                    }
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // estimated time
            btnSetEstimatedTime.setOnClickListener {
                showEstimatedTimeDialog()
            }
            removeEstimatedTime.setOnClickListener {
                viewModel.updateEstimatedWorkTime(null)
            }
            // attach project
            btnAttachProject.setOnClickListener {
                viewModel.navigateToEditParentProject()
            }
            removeProjectLinked.setOnClickListener {
                viewModel.updateParentProject(null)
            }
            // blocking task
            btnChooseDependency.setOnClickListener {
                viewModel.navigateToEditBlockingTaskDialog()
            }
            removeBlockingTask.setOnClickListener {
                viewModel.updateBlockingTask(null)
            }

            // Bindings
            setupDateBindings()
            setupRecurringTaskBinding()
        }
    }

    private fun setupDateBindings() {
        binding.btnSetStartDate.bindDateSelection(
            viewModel.uiState.value.startDate,
            validateDate = ::validateSelectionStartDate,
            getConstraints = {
                CalendarCustomFunction.buildConstraintsForStartDate(
                    viewModel.uiState.value.dueDate ?: viewModel.uiState.value.deadline
                )
            },
            updateFn = { newStartDate -> viewModel.updateStartDate(newStartDate) }
        )

        binding.removeStartDate.setOnClickListener {
            viewModel.updateStartDate(null)
        }

        binding.btnSetDueDate.bindDateSelection(
            viewModel.uiState.value.dueDate,
            validateDate = ::validateSelectionDueDate,
            getConstraints = {
                CalendarCustomFunction.buildConstraintsForDueDate(
                    viewModel.uiState.value.startDate ?: 0L, viewModel.uiState.value.deadline
                )
            },
            updateFn = { newDueDate -> viewModel.updateDueDate(newDueDate) }
        )

        binding.removeDueDate.setOnClickListener {
            viewModel.updateDueDate(null)
        }

        binding.btnSetDeadline.bindDateSelection(
            viewModel.uiState.value.deadline,
            validateDate = ::validateSelectionDeadline,
            getConstraints = {
                CalendarCustomFunction.buildConstraintsForDeadline(
                    viewModel.uiState.value.dueDate ?: viewModel.uiState.value.startDate ?: 0L
                )
            },
            updateFn = { newDeadline -> viewModel.updateDeadline(newDeadline) }
        )

        binding.removeDeadline.setOnClickListener {
            viewModel.updateDeadline(null)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .map { it.error }
                .distinctUntilChanged()
                .collect { error ->
                    error?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun setupRecurringTaskBinding() {
        // set repeatable task
        // TODO: create a custom interval for learning category tasks
        // TODO: update start date and stopAndReset on deadline
        val dropDownMenuRepeat = PopupMenu(requireContext(), binding.btnRepeatTask)
        binding.btnRepeatTask.setOnClickListener {
            dropDownMenuRepeat.show()
        }
        dropDownMenuRepeat.menuInflater.inflate(
            R.menu.popup_menu_repeatable,
            dropDownMenuRepeat.menu
        )
        dropDownMenuRepeat.setOnMenuItemClickListener {
            updateRecurrenceInfos(it.itemId)
            true
        }
        binding.removeRepeatedChoice.setOnClickListener {
            viewModel.updateRecurrenceInfos(null)
        }
    }

    private fun updateRecurrenceInfos(itemId: Int) {
        when (itemId) {
            R.id.action_every_day -> {
                viewModel.updateRecurrenceInfos(
                    TaskRecurrenceWithDays(
                        TaskRecurrence(
                            Period.DAYS.name,
                            1,
                            startDate = null
                        ), emptyList()
                    )
                )
            }

            R.id.action_every_week -> {
                viewModel.updateRecurrenceInfos(
                    TaskRecurrenceWithDays(
                        TaskRecurrence(
                            Period.WEEKS.name,
                            1,
                            startDate = null
                        ), emptyList()
                    )
                )
                Log.e(
                    "task recurrence value",
                    viewModel.uiState.value.taskRecurrenceWithDays.toString()
                )
            }

            R.id.action_personalized -> {
                showRecurringTaskDialog()
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { observeUiState() }
                launch { observeEvents() }
            }
        }
    }

    private suspend fun observeUiState() {
        viewModel.uiState.collect { state ->
            renderUiState(state)
        }
    }

    private suspend fun observeEvents() {
        viewModel.addEditTaskEvent.collect { event ->
            handleEvent(event)
        }
    }

    private fun renderUiState(state: AddEditTaskViewModel.UiState) {
        binding.apply {
            // skill level
            if (state.skillLevel != null)
                inputLayoutUserLevel.editText?.setText(state.skillLevel.toString())

            // Mise à jour des dates
            btnSetStartDate.updateDateButton(
                removeStartDate,
                state.startDate,
                getString(R.string.set_start_date)
            )
            btnSetDueDate.updateDateButton(
                removeDueDate,
                state.dueDate,
                getString(R.string.set_due_date)
            )
            btnSetDeadline.updateDateButton(
                removeDeadline,
                state.deadline,
                getString(R.string.set_deadline)
            )

            // Complex Updates
            btnRepeatTask.updateSpecificButtonText(
                removeRepeatedChoice,
                state.taskRecurrenceWithDays?.getRecurringIntervalReadable(resources),
                getString(R.string.repeat)
            )

            btnSetEstimatedTime.updateSpecificButtonText(
                removeEstimatedTime,
                state.estimatedWorkTime?.let {
                    getString(
                        R.string.estimated_time_info,
                        TrackingTimeUtility.getFormattedEstimatedTime(state.estimatedWorkTime)
                    )
                },
                getString(R.string.set_estimated_time)
            )

            btnAttachProject.updateSpecificButtonText(
                removeProjectLinked,
                state.parentProject?.let {
                    getString(R.string.project_linked_text, state.parentProject.title)
                },
                getString(R.string.project_linked_default)
            )
            // blocking task
            // TODO: add multiple blocking tasks  (or dependency tasks)
            btnChooseDependency.updateSpecificButtonText(
                removeBlockingTask,
                state.blockingTask?.let {
                    getString(R.string.blocking_task, it.title)
                },
                getString(R.string.dependency)
            )
        }
    }

    private fun handleEvent(event: AddEditTaskViewModel.AddEditTaskEvent) {
        when (event) {
            is AddEditTaskViewModel.AddEditTaskEvent.NavigateBackWithResult -> {
                clearFocus()
                sharedViewModel.showConfirmationMessage(event.result)
                findNavController().popBackStack()
            }

            is AddEditTaskViewModel.AddEditTaskEvent.ShowInvalidInputMessage -> Snackbar.make(
                requireView(),
                event.msg,
                Snackbar.LENGTH_LONG
            ).show()

            is AddEditTaskViewModel.AddEditTaskEvent.ScheduleReminders -> {
                Log.e("reminder schedule", "event received and function 'schedule reminders' call")
                scheduleReminders(event.reminders)
            }

            is AddEditTaskViewModel.AddEditTaskEvent.NavigateToEditParentProjectDialog -> {
                val action =
                    AddEditTaskFragmentDirections.actionAddEditTaskFragmentToEditProjectLinkedDialogFragment(
                        event.taskId ?: -1,
                        event.parentProject
                    )
                findNavController().navigate(action)
            }

            is AddEditTaskViewModel.AddEditTaskEvent.NavigateToEditBlockingTaskDialog -> {
                val action =
                    AddEditTaskFragmentDirections.actionAddEditTaskFragmentToEditBlockingTaskDialogFragment(
                        event.taskId ?: -1,
                        event.blockingTask
                    )
                findNavController().navigate(action)
            }
        }
    }

    private fun validateSelectionStartDate(newStartDate: Long): Boolean {
        val state = viewModel.uiState.value
        return when {
            state.dueDate != null && newStartDate > state.dueDate || state.deadline != null && newStartDate > state.deadline -> {
                viewModel.setError(getString(R.string.msg_invalid_start_date))
                false
            }

            else -> true
        }
    }

    private fun validateSelectionDueDate(newDueDate: Long): Boolean {
        val state = viewModel.uiState.value
        return when {
            state.startDate != null && newDueDate < state.startDate || state.deadline != null && newDueDate > state.deadline -> {
                viewModel.setError(getString(R.string.msg_invalid_start_date))
                false
            }

            else -> true
        }
    }

    private fun validateSelectionDeadline(newDeadline: Long): Boolean {
        val state = viewModel.uiState.value
        return when {
            state.startDate != null && newDeadline < state.startDate || state.dueDate != null && newDeadline < state.dueDate -> {
                viewModel.setError(getString(R.string.msg_invalid_start_date))
                false
            }

            else -> true
        }
    }


    // Extensions.kt
    private fun EditText.bindTextTo(updateFn: (String) -> Unit) {
        doAfterTextChanged { text ->
            updateFn(text?.toString() ?: "")
        }
    }

    private fun MaterialButton.bindDateSelection(
        date: Long?,
        validateDate: (Long) -> Boolean,
        getConstraints: () -> CalendarConstraints,
        updateFn: (Long) -> Unit
    ) {
        val dropDownMenuRepeat = PopupMenu(requireContext(), this)
        setOnClickListener {
            dropDownMenuRepeat.show()
        }
        dropDownMenuRepeat.menuInflater.inflate(
            R.menu.popup_menu_date_picker,
            dropDownMenuRepeat.menu
        )
        dropDownMenuRepeat.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_today -> updateFn(Calendar.getInstance().timeInMillis)

                R.id.action_tomorrow -> updateFn(Calendar.getInstance().run {
                    add(Calendar.DAY_OF_MONTH, 1)
                    timeInMillis
                })

                R.id.action_personalized -> {
                    showDatePicker(
                        initialDate = date,
                        constraints = getConstraints(),
                        onDateSelected = { newDate ->
                            if (validateDate(newDate)) {
                                updateFn(newDate)
                            }
                        }
                    )
                }
            }
            true
        }
    }

    private fun MaterialButton.updateDateButton(
        removeButton: View,
        date: Long?,
        defaultText: String
    ) {
        if (date != null) {
            text = SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(date)
            setTextColor(MaterialColors.getColor(context, R.attr.colorPrimary, ""))
            removeButton.isVisible = true
        } else {
            text = defaultText
            setTextColor(MaterialColors.getColor(context, R.attr.colorPrimaryInverse, ""))
            removeButton.isVisible = false
        }
    }

    private fun MaterialButton.updateSpecificButtonText(
        removeButton: View,
        valueText: String?,
        defaultText: String
    ) {
        if (InputValidation.isValidText(valueText)) {
            setTextColor(MaterialColors.getColor(requireView(), R.attr.colorPrimary))
            removeButton.isVisible = true
            text = valueText
        } else {
            setTextColor(MaterialColors.getColor(requireView(), R.attr.colorPrimaryInverse))
            removeButton.isVisible = false
            text = defaultText
        }
    }

    fun Fragment.showDatePicker(
        initialDate: Long? = null,
        constraints: CalendarConstraints? = null,
        onDateSelected: (Long) -> Unit
    ) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(initialDate ?: MaterialDatePicker.todayInUtcMilliseconds())
            .apply { constraints?.let { setCalendarConstraints(it) } }
            .build()

        datePicker.addOnPositiveButtonClickListener { date ->
            val selectedDate = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis
            onDateSelected(selectedDate)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER_TAG")
    }

    private fun clearFocus() {
        binding.editTextName.clearFocus()
        binding.editTextPriority.clearFocus()
        binding.editTextDescription.clearFocus()
    }

    private fun showEstimatedTimeDialog() {
        val newFragment = TimePickerDialogFragment(getString(R.string.set_estimated_time))
        newFragment.show(parentFragmentManager, ESTIMATED_TIME_DIALOG_TAG)
    }

    private fun getStringFromLong(long: Long): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(long)
    }

    private fun setUpButtonDetails(layoutMode: LayoutMode) {
        binding.apply {
            radioGroupChoiceNature.isVisible = layoutMode.name != LayoutMode.SIMPLIFIED.name
            startDateSelectionGroup.isVisible = layoutMode.name != LayoutMode.SIMPLIFIED.name
            deadlineSelectionGroup.isVisible = layoutMode.name != LayoutMode.SIMPLIFIED.name
            estimationSelectionGroup.isVisible = layoutMode.name != LayoutMode.SIMPLIFIED.name
            projectSelectionGroup.isVisible = layoutMode.name != LayoutMode.SIMPLIFIED.name
            dependencySelectionGroup.isVisible = layoutMode.name != LayoutMode.SIMPLIFIED.name
            inputLayoutUserLevel.isVisible = layoutMode.name != LayoutMode.SIMPLIFIED.name
        }
    }

    private fun safeSave(modeExtent: Boolean) {
        if (InputValidation.isValidText(binding.editTextName.text))
            viewModel.saveTask(modeExtent)
        else binding.inputLayoutName.error = getString(R.string.error_no_title)
    }

    private fun scheduleReminders(reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            if (!reminder.isPassed()) {
                val intent = Intent(requireContext(), ReminderBroadcastReceiver::class.java).apply {
                    putExtra(TASK_NAME_KEY, viewModel.uiState.value.title)
                    putExtra(REMINDER_DUE_DATE, reminder.dueDate)
                    putExtra(REMINDER_ID, reminder.id)
                    putExtra(REMINDER_CUSTOM_INTERVAL, reminder.repetitionFrequency)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    reminder.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                Log.e("reminder schedule", viewModel.uiState.value.title + " reminder schedule")
                (requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                    .setExact(AlarmManager.RTC_WAKEUP, reminder.dueDate, pendingIntent)
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

    private fun showCategoryDialog() {
        val newFragment = EditCategoryDialogFragment()
        newFragment.show(parentFragmentManager, CATEGORY_EDIT_TAG)
    }

    private fun showRecurringTaskDialog() {
        val newFragment = EditRecurringDialogFragment()
        val result = Bundle().apply {
            putParcelable(
                "task_recurrence_with_days",
                viewModel.uiState.value.taskRecurrenceWithDays
            )
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
    }
}

