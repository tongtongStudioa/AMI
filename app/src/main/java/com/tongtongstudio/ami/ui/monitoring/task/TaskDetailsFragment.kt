package com.tongtongstudio.ami.ui.monitoring.task

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.FragmentTaskInformationBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TaskDetailsFragment : Fragment(R.layout.fragment_task_information) {

    lateinit var binding: FragmentTaskInformationBinding
    private val viewModel: TaskDetailsAndTimeTrackerViewModel by lazy {
        if (parentFragment is ViewPagerTrackingAndStatsFragment) { // when inside view pager
            ViewModelProvider(requireParentFragment())[TaskDetailsAndTimeTrackerViewModel::class.java]
        } else {
            ViewModelProvider(this)[TaskDetailsAndTimeTrackerViewModel::class.java]
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTaskInformationBinding.bind(view)

        // binding elements layout
        binding.apply {
            // task info
            if (viewModel.name != null) {
                taskName.text = viewModel.task!!.title
            }
            taskCategory.text = viewModel.category
            taskDescription.text = viewModel.description
            taskDescription.isVisible = viewModel.description != null
            taskStartDate.text = Ttd.getDateFormatted(viewModel.startDate)
            taskStartDate.isVisible = viewModel.startDate != null
            taskDueDate.text =
                Ttd.getDateFormatted(viewModel.dueDate)
            taskDeadline.text = Ttd.getDateFormatted(viewModel.deadline)
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
}