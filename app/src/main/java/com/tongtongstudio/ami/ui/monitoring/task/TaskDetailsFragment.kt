package com.tongtongstudio.ami.ui.monitoring.task

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.transition.MaterialContainerTransform
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.Type
import com.tongtongstudio.ami.databinding.FragmentTaskDetailsBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.util.CalendarCustomFunction
import com.tongtongstudio.ami.util.DateTimePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar


@AndroidEntryPoint
class TaskDetailsFragment : Fragment(R.layout.fragment_task_details) {

    lateinit var binding: FragmentTaskDetailsBinding
    lateinit var dateTimePicker: DateTimePicker
    private val viewModel: TaskDetailsAndTimeTrackerViewModel by lazy {
        if (parentFragment is ViewPagerTrackingAndStatsFragment) { // when inside view pager
            ViewModelProvider(requireParentFragment())[TaskDetailsAndTimeTrackerViewModel::class.java]
        } else {
            ViewModelProvider(this)[TaskDetailsAndTimeTrackerViewModel::class.java]
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // for calendar getInstance function
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.long_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
        // Shared transition id
        ViewCompat.setTransitionName(binding.taskInfo, "shared_element_${viewModel.taskId}")

        // Postpone transition until layout is ready
        //startPostponedEnterTransition()

        // show or hide app bar if fragment is in unique mode
        if (parentFragment !is ViewPagerTrackingAndStatsFragment) {
            binding.appBar.isVisible = true
            setUpToolbar()
        } else {
            binding.appBar.isVisible = false
        }

        dateTimePicker = DateTimePicker(parentFragmentManager, requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect {
                        renderUiState(it)
                    }
                }
            }
        }

        binding.btnCompletionDate.setOnClickListener {
            onCompletionDateBtnClicked()
        }
    }

    private fun renderUiState(uiState: DetailUiState) {
        binding.apply {
            val mainTask = uiState.thingToDo?.taskRelations?.mainTask
            // task info
            taskName.text = mainTask?.title ?: ""
            taskCategory.text = uiState.thingToDo?.taskRelations?.category?.title ?: ""
            taskNature.text = uiState.thingToDo?.getNature(requireContext())
            taskStatus.text = uiState.thingToDo?.taskRelations?.mainTask?.status ?: ""
            taskType.text = uiState.thingToDo?.getType()
            taskDescription.text = mainTask?.description
            taskDescription.isVisible = mainTask?.description != null
            description.isVisible = mainTask?.description != null
            taskStartDate.text = Task.getDateFormatted(mainTask?.startDate)
            taskStartDate.isVisible = mainTask?.startDate != null
            taskDueDate.text =
                Task.getDateFormatted(mainTask?.dueDate)
            taskDeadline.text = Task.getDateFormatted(mainTask?.deadline)
            taskDeadline.isVisible = mainTask?.deadline != null

            // total work time
            viewModel.currentTotalWorkTime.observe(viewLifecycleOwner) {
                totalDurationView.isVisible = it != null
                tvTotalDuration.text =
                    TrackingTimeUtility.getFormattedTimeWorked(it)
                        ?: getString(R.string.no_information)
            }

            // estimated work time view when task is completed
            tvEstimatedWorkTime.text =
                TrackingTimeUtility.getFormattedEstimatedTime(mainTask?.estimatedWorkingTime)
                    ?: getString(R.string.no_information)

            // stats view
            tvSessionsCount.text = uiState.workSessionsCount.toString()
            tvMeanDuration.text =
                TrackingTimeUtility.getFormattedTimeWorked(uiState.meanWorkSessionDuration.toLong())
                    ?: getString(R.string.no_information)
            priorityIndex.isVisible = uiState.priorityIndex != -1 && uiState.priorityIndex != null
            tvPriorityIndex.text = uiState.priorityIndex.toString()
            effectiveStartDate.isVisible = uiState.effectiveStartDate != null
            tvEffectiveStartDate.text =
                Task.getDateFormatted(uiState.effectiveStartDate)
                    ?: getString(R.string.no_information)

            // recurring stats view
            val isRecurrentTask = uiState.thingToDo?.getType() != Type.UNIQUE.name
            recurringStatsView.isVisible = isRecurrentTask

            // completion date
            btnCompletionDate.isVisible = uiState.taskCompletion?.isCompleted ?: false
            // TODO: change this for recurring task : show calendar view where we can update all completions
            val completionDateFormatted = uiState.taskCompletion?.getCompletionDateFormatted()
            btnCompletionDate.text =
                getString(R.string.completion_date, completionDateFormatted)
        }

        // render recurring elements layout
        binding.apply {
            // TODO: change this with function in viewModel to get stats and graph
            tvNbCompleted.text = if (uiState.successCount != null) uiState.successCount.toString()
            else getString(R.string.no_information)
            tvStreak.text = uiState.currentStreak.toString()
            tvMaxStreak.text =
                if (uiState.maxStreak != null)
                    uiState.maxStreak.toString()
                else getString(R.string.no_information)

            val completionRate = uiState.completionRate
            tvCompletionRate.text = if (completionRate != null)
                getString(R.string.completion_rate_value, completionRate)
            else getString(R.string.no_information)
        }
    }

    private fun onCompletionDateBtnClicked() {
        val constraints =
            CalendarCustomFunction.buildConstraintsForStartDate(Calendar.getInstance().run {
                set(Calendar.HOUR_OF_DAY, 23)
                timeInMillis
            })
        val datePicker = dateTimePicker.showDatePickerMaterial(
            constraints
        )
        datePicker.addOnPositiveButtonClickListener { newCompletionDate ->
            viewModel.updateTaskCompletionDate(newCompletionDate)
            binding.btnCompletionDate.text = getString(
                R.string.completion_date,
                DateFormat.getDateInstance().format(newCompletionDate)
            )
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
    }

}