package com.tongtongstudio.ami.ui.monitoring.task

import android.graphics.Color
import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.transition.MaterialContainerTransform
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.databinding.FragmentTaskDetailsBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.util.CalendarCustomFunction
import com.tongtongstudio.ami.util.DateTimePicker
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TaskDetailsFragment : Fragment(R.layout.fragment_task_details) {

    lateinit var binding: FragmentTaskDetailsBinding
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
        ViewCompat.setTransitionName(binding.taskInfo, "shared_element_${viewModel.task?.id}")

        // show or hide app bar if fragment is in unique mode
        if (parentFragment !is ViewPagerTrackingAndStatsFragment) {
            binding.appBar.isVisible = true
            setUpToolbar()
        } else {
            binding.appBar.isVisible = false
        }

        // binding elements layout
        binding.apply {
            // task info
            if (viewModel.name != null) {
                taskName.text = viewModel.task!!.title
            }
            taskCategory.text = viewModel.category
            taskDescription.text = viewModel.description
            taskDescription.isVisible = viewModel.description != null
            taskStartDate.text = Task.getDateFormatted(viewModel.startDate)
            taskStartDate.isVisible = viewModel.startDate != null
            taskDueDate.text =
                Task.getDateFormatted(viewModel.dueDate)
            taskDeadline.text = Task.getDateFormatted(viewModel.deadline)
            taskDeadline.isVisible = viewModel.deadline != null

            // stats view
            if (!viewModel.task?.isRecurring!!)
                statsView.isVisible = false

            tvNbCompleted.text = if (viewModel.task?.successCount != null)
                viewModel.task?.successCount.toString()
            else getString(R.string.no_information)
            tvStreak.text =
                viewModel.streak.toString()
            tvMaxStreak.text =
                if (viewModel.task?.maxStreak != null)
                    viewModel.task?.maxStreak.toString()
                else getString(R.string.no_information)

            val completionRate = viewModel.task?.getHabitSuccessRate()
            tvCompletionRate.text = if (completionRate != null)
                getString(R.string.completion_rate_value, completionRate)
            else getString(R.string.no_information)

            viewModel.currentTotalWorkTime.observe(viewLifecycleOwner) {
                totalDurationView.isVisible = it != null
                tvTotalDuration.text =
                    TrackingTimeUtility.getFormattedTimeWorked(it)
                        ?: getString(R.string.no_information)
            }

            // estimated work time view when task is completed
            tvEstimatedWorkTime.text =
                TrackingTimeUtility.getFormattedEstimatedTime(viewModel.estimatedWorkingTime)
                    ?: getString(R.string.no_information)
            estimatedWorkTimeView.isVisible =
                viewModel.estimatedWorkingTime != null

            // completion date
            val dateTimePicker = DateTimePicker(parentFragmentManager, requireContext())
            val completionDateFormatted = viewModel.task?.getCompletionDateFormatted()
            completionDate.text = getString(R.string.completion_date, completionDateFormatted)
            completionDate.isVisible = viewModel.task?.isCompleted == true
            //completionDate.isVisible = viewModel.task?.isCompleted == true
            completionDate.setOnClickListener {
                val constraints =
                    CalendarCustomFunction.buildConstraintsForStartDate(Calendar.getInstance().run {
                        set(Calendar.HOUR_OF_DAY, 23)
                        timeInMillis
                    })
                val datePicker = dateTimePicker.showDatePickerMaterial(
                    constraints,
                    viewModel.task?.completionDate
                )
                datePicker.addOnPositiveButtonClickListener { newCompletionDate ->
                    viewModel.updateTaskCompletionDate(newCompletionDate)
                    completionDate.text = getString(
                        R.string.completion_date,
                        DateFormat.getDateInstance().format(newCompletionDate)
                    )
                }
            }
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