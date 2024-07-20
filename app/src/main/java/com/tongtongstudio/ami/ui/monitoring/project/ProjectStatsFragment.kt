package com.tongtongstudio.ami.ui.monitoring.project

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.FragmentProjectStatsBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProjectStatsFragment : Fragment(R.layout.fragment_project_stats) {
    lateinit var binding: FragmentProjectStatsBinding
    private val viewModel: ProjectStatsViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectStatsBinding.bind(view)
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setUpToolbar()
        updateProgressBar()
        binding.estimatedTime.text =
            TrackingTimeUtility.getFormattedTimeWorked(viewModel.estimatedTime)
                ?: getText(R.string.no_information)
        binding.totalWorkTime.text =
            TrackingTimeUtility.getFormattedTimeWorked(viewModel.workTime)
                ?: getText(R.string.no_information)
        updateEstimatedTimeIndicator()

        val subTasksAdapter = EditAttributesAdapter<Ttd>(object : AttributeListener<Ttd> {
            override fun onItemClicked(attribute: Ttd) {
                // TODO: go to task information
            }

            override fun onRemoveCrossClick(attribute: Ttd) {
                viewModel.deleteSubtask(attribute)
                sharedViewModel.updateParentTask(attribute.parentTaskId!!)
                Snackbar.make(view, getString(R.string.msg_sub_task_deleted), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.msg_action_undo)) {
                        viewModel.onUndoClick(attribute)
                        sharedViewModel.updateParentTask(attribute.parentTaskId)
                    }.show()
            }

        }
        ) { binding, task ->
            binding.titleOverview.text = task.title
        }
        subTasksAdapter.submitList(viewModel.subTasks)

        binding.rvSubtasks.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = subTasksAdapter
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

    private fun updateEstimatedTimeIndicator() {
        if ((viewModel.estimatedTime ?: 0) < viewModel.workTime)
            binding.estimatedTime.setTextColor(resources.getColor(R.color.design_default_color_error))
    }

    private fun updateProgressBar() {
        var sum = 0
        for (task in viewModel.subTasks) {
            sum += task.priority * if (task.isCompleted) 1 else 0
        }
        val progress =
            sum / (if (viewModel.subTasks.isEmpty()) 1F else viewModel.subTasks.sumOf { it.priority }
                .toFloat()) * 100
        // Update project progress
        binding.projectProgress.progress = progress.toInt()

        // Update text progress
        binding.progressText.text = getString(R.string.completion_rate_value, progress)

        //if (progress == 100F)
        //make a sound
    }

}