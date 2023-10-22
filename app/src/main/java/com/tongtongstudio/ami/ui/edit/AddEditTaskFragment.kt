package com.tongtongstudio.ami.ui.edit

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.core.app.AlarmManagerCompat
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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.RecurringTaskInterval
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import com.tongtongstudio.ami.databinding.AddEditTaskFragmentBinding
import com.tongtongstudio.ami.receiver.AlarmReceiver
import com.tongtongstudio.ami.receiver.TASK_CHANNEL_ID
import com.tongtongstudio.ami.receiver.TTD_DESCRIPTION
import com.tongtongstudio.ami.receiver.TTD_NAME
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.dialog.*
import com.tongtongstudio.todolistami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class AddEditTaskFragment : Fragment(R.layout.add_edit_task_fragment) {

    private val viewModel: AddEditTaskViewModel by viewModels()

    private lateinit var binding: AddEditTaskFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (viewModel.thingToDo == null) createNotificationChannel()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = AddEditTaskFragmentBinding.bind(view)

        // set toolbar with menu and navigate up icon
        setUpToolbar()

        // set view on the page
        updateBtnDeadlineText()
        updateBtnStartDateText()
        updateBtnEstimatedTimeText()
        updateBtnReminderText()
        updatePeriodRecurringTask()

        binding.apply {

            radioGroupChoiceNature.check(
                when (viewModel.ttdNature) {
                    Nature.PROJECT -> {
                        rbProject.id
                    }
                    Nature.EVENT -> {
                        rbEvent.id
                    }
                    Nature.TASK -> {
                        rbTask.id
                    }
                }
            )
            radioGroupChoiceNature.jumpDrawablesToCurrentState()

            radioGroupChoiceNature.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    rbEvent.id -> {
                        viewModel.ttdNature = Nature.EVENT
                        evaluationView.isVisible = false
                    }
                    rbTask.id -> {
                        viewModel.ttdNature = Nature.TASK
                        evaluationView.isVisible = false
                    }
                    rbProject.id -> {
                        viewModel.ttdNature = Nature.PROJECT
                        evaluationView.isVisible = false
                    }
                }
            }

            // text view created date
            textViewCreatedDate.isVisible = viewModel.thingToDo != null
            textViewCreatedDate.text = if (viewModel.createdDateFormatted != null)
                getString(R.string.text_created_date, viewModel.createdDateFormatted) else ""

            // edit name
            editTextName.setText(viewModel.name)
            editTextName.addTextChangedListener {
                val name = it.toString()
                viewModel.name = if (name != "") name.replaceFirst(
                    name.first(),
                    name.first().uppercaseChar()
                ) else ""
            }

            // edit priority
            editTextPriority.setText(viewModel.priority.replace("null", ""))
            editTextPriority.addTextChangedListener {
                viewModel.priority = if (it.toString() == "null") "" else it.toString()
            }

            // edit category
            if (viewModel.category != null)
                autocompleteTextCategory.setText(viewModel.category)
            val categoryOptions = arrayOf("No category", "Professional", "Learning", "Personal")
            val adapterCategory =
                ArrayAdapter(requireContext(), R.layout.list_options, categoryOptions)
            autocompleteTextCategory.setAdapter(adapterCategory)
            autocompleteTextCategory.doOnTextChanged { text, start, before, count ->
                if (text.toString() != "No category")
                    viewModel.category = text.toString()
                else viewModel.category = null
            }

            // set start date
            val dropDownMenuStartDate = PopupMenu(context, btnSetStartDate)
            btnSetStartDate.setOnClickListener {
                dropDownMenuStartDate.show()
            }
            dropDownMenuStartDate.menuInflater.inflate(
                R.menu.popup_menu_date_picker,
                dropDownMenuStartDate.menu
            )
            dropDownMenuStartDate.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_today -> {
                        val todayDate = Calendar.getInstance().time
                        if (viewModel.deadline == null || viewModel.deadline!! > todayDate.time) {
                            viewModel.startDate = todayDate.time
                            updateBtnStartDateText()
                        } else viewModel.showInvalidInputMessage("Start date must be set before deadline")
                        true
                    }
                    R.id.action_tomorrow -> {
                        val tomorrowDate = Calendar.getInstance().run {
                            add(Calendar.DAY_OF_MONTH, 1)
                            timeInMillis
                        }
                        if (viewModel.deadline == null || viewModel.deadline!! > tomorrowDate) {
                            viewModel.startDate = tomorrowDate
                            updateBtnStartDateText()
                        } else viewModel.showInvalidInputMessage("Start date must be set before deadline")
                        true
                    }
                    R.id.action_personalized -> {
                        pickCustomStartDate(viewModel.startDate)
                        true
                    }
                    else -> true
                }
            }
            removeStartDate.setOnClickListener {
                viewModel.startDate = null
                viewModel.isRecurring = false
                viewModel.recurringTaskInterval = null
                updateBtnStartDateText()
                updatePeriodRecurringTask()
            }

            // set deadline
            // TODO: link with selection of repeatable schema to stop it
            val dropDownMenuDeadline = PopupMenu(context, btnSetDeadline)
            btnSetDeadline.setOnClickListener {
                dropDownMenuDeadline.show()
            }
            dropDownMenuDeadline.menuInflater.inflate(
                R.menu.popup_menu_date_picker,
                dropDownMenuDeadline.menu
            )
            dropDownMenuDeadline.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_today -> {
                        val todayDate = Calendar.getInstance().time
                        if (viewModel.startDate == null || viewModel.startDate!! < todayDate.time) {
                            viewModel.deadline = todayDate.time
                            updateBtnDeadlineText()
                        } else viewModel.showInvalidInputMessage("Start date must be set before deadline")
                        true
                    }
                    R.id.action_tomorrow -> {
                        val tomorrowDate = Calendar.getInstance().run {
                            add(Calendar.DAY_OF_MONTH, 1)
                            time
                        }
                        if (viewModel.startDate == null || viewModel.startDate!! < tomorrowDate.time) {
                            viewModel.deadline = tomorrowDate.time
                            updateBtnDeadlineText()
                        } else viewModel.showInvalidInputMessage("Start date must be set before deadline")
                        true
                    }
                    R.id.action_personalized -> {
                        pickCustomDeadline(viewModel.deadline)
                        true
                    }
                    else -> true
                }
            }
            removeDeadline.setOnClickListener {
                viewModel.deadline = null
                updateBtnDeadlineText()
            }

            // set reminder
            btnSetReminder.setOnClickListener {
                var reminderTriggerTime: Long
                // create the calendar constraint builder
                val calendarConstraintBuilder = CalendarConstraints.Builder()
                if (viewModel.deadline != null)
                    calendarConstraintBuilder.setValidator(DateValidatorPointBackward.before(viewModel.deadline!!))
                val datePicker = showDatePickerMaterial(
                    viewModel.deadline,
                    calendarConstraintBuilder.build()
                )

                datePicker.addOnPositiveButtonClickListener {
                    val date = Date(it)
                    val cal = Calendar.getInstance()
                    cal.time = date

                    val timePicker = showTimePickerMaterial()
                    timePicker.addOnPositiveButtonClickListener {
                        val pickedHour = timePicker.hour
                        val pickedMinutes = timePicker.minute
                        cal.set(Calendar.HOUR_OF_DAY, pickedHour)
                        cal.set(Calendar.MINUTE, pickedMinutes)
                        reminderTriggerTime = cal.timeInMillis

                        viewModel.reminder = reminderTriggerTime
                        updateBtnReminderText()
                    }
                }
            }
            removeReminder.setOnClickListener {
                viewModel.reminder = null
                updateBtnReminderText()
            }

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
                        viewModel.recurringTaskInterval = RecurringTaskInterval(1,Period.DAYS.name)
                        updatePeriodRecurringTask()
                        true
                    }
                    R.id.action_every_week -> {
                        viewModel.isRecurring = true
                        viewModel.recurringTaskInterval = RecurringTaskInterval(1,Period.WEEKS.name)
                        updatePeriodRecurringTask()
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
                updatePeriodRecurringTask()
            }

            // set estimated work time
            btnSetEstimatedTime.setOnClickListener {
                showEstimatedTimePicker()
            }
            removeEstimatedTime.setOnClickListener {
                viewModel.estimatedTime = null
                updateBtnEstimatedTimeText()
            }

            // edit description
            if (viewModel.description != null)
                inputLayoutDescription.editText?.setText(viewModel.description)
            inputLayoutDescription.editText?.doOnTextChanged { text, start, before, count ->
                viewModel.description = text.toString()
            }

            // evaluation description
            if (viewModel.evaluationTaskDescription != null)
                inputLayoutEvaluation.editText?.setText(viewModel.evaluationTaskDescription)
            inputLayoutEvaluation.editText?.doOnTextChanged { text, start, before, count ->
                viewModel.evaluationTaskDescription = text.toString()
            }
            // evaluation rating
            if (viewModel.evaluationTaskGoal != null)
                inputLayoutGoal.editText?.setText(viewModel.evaluationTaskGoal.toString())
            inputLayoutGoal.editText?.addTextChangedListener {
                if (it.toString() != "null" && it.toString() != "")
                    viewModel.evaluationTaskGoal = it.toString().toDouble()
            }
            // evaluation unit
            if (viewModel.evaluationTaskUnit != null)
                autocompleteTextViewUnit.setText(viewModel.evaluationTaskUnit)
            val unitOptions = arrayOf("Number", "Kg", "Minutes")
            val unitAdapter =
                ArrayAdapter(requireContext(), R.layout.list_options, unitOptions)
            autocompleteTextViewUnit.setAdapter(unitAdapter)
            autocompleteTextViewUnit.doOnTextChanged { text, start, before, count ->
                viewModel.evaluationTaskUnit = text.toString()
            }
            // evaluation date
            if (viewModel.evaluationTaskDate != null)
                setEvaluationDate.text =
                    DateFormat.getDateInstance().format(viewModel.evaluationTaskDate)
            setEvaluationDate.setOnClickListener {
                pickCustomEvaluationDate(viewModel.deadline)
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
                updateBtnEstimatedTimeText()
            }
        }

        // from dialog recurring selection
        setFragmentResultListener(RECURRING_REQUEST_KEY) { _, bundle ->
            val result = bundle.getParcelable<RecurringTaskInterval>(RECURRING_RESULT_KEY)
            viewModel.recurringTaskInterval = result
            viewModel.isRecurring = result != null
            // update task start date if not set
            if (result != null) {
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
                updateBtnStartDateText()
            }
            updatePeriodRecurringTask()
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

    }

    private fun pickCustomEvaluationDate(deadline: Long?) {
        // create the calendar constraint builder
        val calendarConstraintBuilder = CalendarConstraints.Builder()
        if (viewModel.startDate != null) {
            calendarConstraintBuilder.setValidator(DateValidatorPointForward.from(viewModel.startDate!!))
        }
        if (viewModel.deadline != null) {
            calendarConstraintBuilder.setValidator(DateValidatorPointBackward.before(viewModel.deadline!!))
        }

        val datePicker = showDatePickerMaterial(deadline,calendarConstraintBuilder.build())
        datePicker.addOnPositiveButtonClickListener {
            viewModel.evaluationTaskDate = it
            binding.setEvaluationDate.text = DateFormat.getDateInstance().format(it)
        }
    }

    // TODO: change color text when dark mode is activated
    private fun updateBtnReminderText() {
        binding.apply {
            if (viewModel.reminder != null) {
                btnSetReminder.setIconTintResource(R.color.french_blue)
                btnSetReminder.setTextColor(getColor(requireContext(), R.color.french_blue))
                btnSetReminder.text = DateFormat.getDateInstance()
                    .format(viewModel.reminder!!)
                removeReminder.isVisible = true
            } else {
                btnSetReminder.text = getString(R.string.set_reminder)
                btnSetReminder.setIconTintResource(R.color.boulder)
                btnSetReminder.setTextColor(getColor(requireContext(), R.color.boulder))
                removeReminder.isVisible = false
            }
        }
    }

    private fun updateBtnEstimatedTimeText() {
        binding.apply {
            if (viewModel.estimatedTime != null) {
                btnSetEstimatedTime.setIconTintResource(R.color.french_blue)
                btnSetEstimatedTime.setTextColor(getColor(requireContext(), R.color.french_blue))
                val hours = viewModel.estimatedTime!!.toInt() / 3600_000
                val minutes = viewModel.estimatedTime!!.toInt() / 60_000 % 60
                val minutesToString = if (minutes < 10) "0$minutes" else minutes.toString()
                btnSetEstimatedTime.text =
                    getString(R.string.estimated_time_info, hours, minutesToString)
                removeEstimatedTime.isVisible = true
            } else {
                btnSetEstimatedTime.text = getString(R.string.set_estimated_time)
                btnSetEstimatedTime.setIconTintResource(R.color.boulder)
                btnSetEstimatedTime.setTextColor(getColor(requireContext(), R.color.boulder))
                removeEstimatedTime.isVisible = false
            }
        }
    }

    private fun updateBtnStartDateText() {
        binding.apply {
            btnSetStartDate.text = if (viewModel.startDate != null) {
                btnSetStartDate.setIconTintResource(R.color.french_blue)
                btnSetStartDate.setTextColor(getColor(requireContext(), R.color.french_blue))
                removeStartDate.isVisible = true
                getStringFromLong(viewModel.startDate!!)
            } else {
                btnSetStartDate.setIconTintResource(R.color.boulder)
                btnSetStartDate.setTextColor(getColor(requireContext(), R.color.boulder))
                removeStartDate.isVisible = false
                getString(R.string.set_start_date)
            }
        }
    }

    private fun showEstimatedTimePicker() {
        val newFragment = EstimatedTimeDialogFragment()
        newFragment.show(parentFragmentManager, ESTIMATED_TIME_DIALOG_TAG)
    }

    private fun updateBtnDeadlineText() {
        binding.apply {
            btnSetDeadline.text = if (viewModel.deadline != null) {
                btnSetDeadline.setIconTintResource(R.color.french_blue)
                btnSetDeadline.setTextColor(getColor(requireContext(), R.color.french_blue))
                removeDeadline.isVisible = true
                getStringFromLong(viewModel.deadline!!)
            } else {
                btnSetDeadline.setIconTintResource(R.color.boulder)
                btnSetDeadline.setTextColor(getColor(requireContext(), R.color.boulder))
                removeDeadline.isVisible = false
                getString(R.string.set_deadline)
            }
        }
    }

    // TODO: 11/02/2023 delete this method to free up space
    private fun getStringFromLong(long: Long): String {
        return SimpleDateFormat(PATTERN_FORMAT_DATE,Locale.getDefault()).format(long)
    }

    private fun pickCustomStartDate(startDate: Long?) {
        // create the calendar constraint builder
        val calendarConstraintBuilder = CalendarConstraints.Builder()
        if (viewModel.deadline != null)
            calendarConstraintBuilder.setValidator(DateValidatorPointBackward.before(viewModel.deadline!!))
        val datePicker = showDatePickerMaterial(startDate,calendarConstraintBuilder.build())
        datePicker.addOnPositiveButtonClickListener {
            viewModel.startDate = it
            updateBtnStartDateText()
        }
    }

    private fun pickCustomDeadline(deadline: Long?) {
        // create the calendar constraint builder
        val calendarConstraintBuilder = CalendarConstraints.Builder()
        if (viewModel.startDate != null)
            calendarConstraintBuilder.setValidator(DateValidatorPointForward.from(viewModel.startDate!!))
        val datePicker = showDatePickerMaterial(deadline, calendarConstraintBuilder.build())
        datePicker.addOnPositiveButtonClickListener {
            viewModel.deadline = it
            updateBtnDeadlineText()
        }
    }

    private fun safeSave() {
        if (viewModel.name.isBlank()) {
            //binding.inputLayoutName.error = "Name !"
            viewModel.showInvalidInputMessage(getString(R.string.error_no_name))
            return
        } else if (viewModel.priority.isBlank() || viewModel.priority == "null") {
            viewModel.showInvalidInputMessage(getString(R.string.error_no_priority))
            return
        } else if (viewModel.deadline == null && !viewModel.isRecurring) {
            viewModel.showInvalidInputMessage(getString(R.string.error_no_date))
            return
        }
        scheduleReminder(viewModel.reminder)
        viewModel.onSaveClick()
    }

    private fun scheduleReminder(timeReminder: Long?) {
        if (timeReminder != null && timeReminder > Calendar.getInstance().timeInMillis) {
            val alarmManager =
                activity?.application?.getSystemService(Application.ALARM_SERVICE) as AlarmManager
            val notifyIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(TTD_NAME, viewModel.name)
                putExtra(TTD_DESCRIPTION, getStringFromLong(viewModel.deadline!!))
            }
            val notifyPendingIntent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.getBroadcast(context, 0, notifyIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                } else {
                    PendingIntent.getBroadcast(context, 0, notifyIntent, Intent.FILL_IN_ACTION)
                }

            val recurringInfo = viewModel.recurringTaskInterval
            if (viewModel.isRecurring && recurringInfo != null) {
                val intervalInMillis: Long =
                    when (recurringInfo.period) {
                        Period.DAYS.name -> recurringInfo.times * AlarmManager.INTERVAL_DAY
                        Period.WEEKS.name -> recurringInfo.times * AlarmManager.INTERVAL_DAY * 7
                        Period.MONTHS.name -> recurringInfo.times * AlarmManager.INTERVAL_DAY * 30
                        Period.YEARS.name -> recurringInfo.times * AlarmManager.INTERVAL_DAY * 365
                        else -> recurringInfo.times * AlarmManager.INTERVAL_DAY
                    }
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    timeReminder,
                    intervalInMillis,
                    notifyPendingIntent
                )
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val hasPermission: Boolean = alarmManager.canScheduleExactAlarms()
                    if (!hasPermission) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        }

                        startActivity(intent)
                    }
                }
                AlarmManagerCompat.setExact(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    timeReminder,
                    notifyPendingIntent
                )
            }
        }
    }

    private fun showRepeatableCyclePicker() {
        val newFragment = RecurringChoiceDialogFragment()
        val result = Bundle().apply {
            putInt(TIMES_KEY,viewModel.recurringTaskInterval?.times ?: 0)
            putString(PERIOD_KEY,viewModel.recurringTaskInterval?.period ?: NO_VALUE)
            putIntArray(DAYS_OF_THE_WEEKS_KEY,viewModel.recurringTaskInterval?.daysOfWeek?.toIntArray() ?: IntArray(1))
            putString(DEADLINE, if (viewModel.deadline != null) getStringFromLong(viewModel.deadline!!) else NO_VALUE )
            putLong(START_DATE,if (viewModel.startDate != null) viewModel.startDate!! else 0L)
        }
        setFragmentResult(CURRENT_RECURRING_INFO_REQUEST_KEY, result)
        newFragment.show(parentFragmentManager, RECURRING_SELECTION_DIALOG_TAG)
    }

    private fun updatePeriodRecurringTask() {
        val recurringInfo = viewModel.recurringTaskInterval
        // update btn repeat text
        binding.btnRepeatTask.text =
            if (recurringInfo != null) {
                binding.removeRepeatedChoice.isVisible = true
                binding.btnRepeatTask.setIconTintResource(R.color.french_blue)
                binding.btnRepeatTask.setTextColor(getColor(requireContext(), R.color.french_blue))
                if ( recurringInfo.times == 1 && recurringInfo.daysOfWeek == null) {
                    when (recurringInfo.period){
                      Period.DAYS.name-> getString(R.string.each_days)
                      Period.WEEKS.name -> getString(R.string.each_weeks)
                      Period.MONTHS.name -> getString(R.string.each_months)
                      Period.YEARS.name -> getString(R.string.each_years)
                      else -> getString(R.string.each_days)
                  }
                } else if (recurringInfo.daysOfWeek != null) { // TODO: change this to best integration
                    if (recurringInfo.times == 1) {
                        "Weekly on " + recurringInfo.daysOfWeek

                    } else "On " + recurringInfo.daysOfWeek + " every " + recurringInfo.times + " weeks"
                } else {
                    when (recurringInfo.period) {
                        Period.DAYS.name -> getString(R.string.every_x_days, recurringInfo.times)
                        Period.WEEKS.name -> getString(R.string.every_x_weeks, recurringInfo.times)
                        Period.MONTHS.name -> getString(R.string.every_x_months, recurringInfo.times)
                        Period.YEARS.name -> getString(R.string.every_x_years, recurringInfo.times)
                        else -> getString(R.string.every_x_days, recurringInfo.times)
                    }
                }
            } else {
                binding.removeRepeatedChoice.isVisible = false
                binding.btnRepeatTask.setIconTintResource(R.color.boulder)
                binding.btnRepeatTask.setTextColor(getColor(requireContext(), R.color.boulder))
                getString(R.string.repeat_text)
            }
    }

    private fun showDatePickerMaterial(selection: Long?, constraints: CalendarConstraints): MaterialDatePicker<Long> {
        viewModel.eventIsSpread = false
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
        viewModel.eventIsSpread = true
        val dateRangePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.Select_two_date_to_spread_event))
                .build()
        dateRangePicker.show(parentFragmentManager, "dateRangePicker")

        return dateRangePicker
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.title_notification_channel)
            val descriptionText = getString(R.string.description_notification_channel)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(TASK_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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
        binding.toolbar.setNavigationOnClickListener {
            navController.navigateUp(appBarConfiguration)
        }
    }
}