package com.tongtongstudio.ami.ui.monitoring.task

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.databinding.FragmentTaskDetailsBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTaskDetailsBinding.bind(view)

        // show or hide app bar if fragment is in unique mode
        if (parentFragment !is ViewPagerTrackingAndStatsFragment) {
            binding.appBar.isVisible = true
            setUpToolbar()
        } else binding.appBar.isVisible = false

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

            viewModel.actualWorkTime.observe(viewLifecycleOwner) {
                totalDurationView.isVisible = it != null
                tvTotalDuration.text =
                    TrackingTimeUtility.getFormattedTimeWorked(it)
                        ?: getString(R.string.no_information)
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