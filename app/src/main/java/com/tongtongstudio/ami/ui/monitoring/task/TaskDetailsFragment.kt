package com.tongtongstudio.ami.ui.monitoring.task

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.FragmentTaskInformationBinding
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
                tvTotalDuration.text =
                    Ttd.getFormattedTime(it) ?: getString(R.string.no_information)
            }

            // TODO: show graph and assessments comments
            chartView.isVisible = false
            // in this example, a LineChart is initialized from xml
            val entries = ArrayList<Entry>()
            entries.add(Entry(1.0F, 8.0F))
            entries.add(Entry(2.0F, 12.0F))
            entries.add(Entry(3.0F, 4.0F))
            entries.add(Entry(4.0F, 25.0F))
            entries.add(Entry(5.0F, 26.0F))
            entries.add(Entry(6.0F, 15.0F))
            entries.add(Entry(7.0F, 19.0F))
            entries.add(Entry(8.0F, 27.0F))
            val dataSet = LineDataSet(entries, "1 serie")
            val lineData = LineData(dataSet)
            //chartView.data = lineData
            chartView.invalidate() // refresh
        }
    }
}