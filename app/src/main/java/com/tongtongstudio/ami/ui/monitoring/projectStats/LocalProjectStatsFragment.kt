package com.tongtongstudio.ami.ui.monitoring.projectStats

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.AttributeListener
import com.tongtongstudio.ami.adapter.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.ProjectStatsFragmentBinding
import com.tongtongstudio.ami.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class LocalProjectStatsFragment : Fragment(R.layout.project_stats_fragment) {
    lateinit var binding: ProjectStatsFragmentBinding
    private val viewModel: ProjectStatsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ProjectStatsFragmentBinding.bind(view)

        setUpToolbar()
        updateProgressBar()
        binding.estimatedTime.text =
            getDurationFromLong(viewModel.estimatedTime) ?: getText(R.string.no_information)
        binding.totalWorkTime.text =
            getDurationFromLong(viewModel.workTime) ?: getText(R.string.no_information)
        updateEstimatedTimeIndicator()

        val subTasksAdapter = EditAttributesAdapter<Ttd>(object : AttributeListener<Ttd> {
            override fun onItemClicked(attribute: Ttd) {
                TODO("Not yet implemented")
            }

            override fun onRemoveCrossClick(attribute: Ttd) {
                viewModel.deleteSubtask(attribute)
                Snackbar.make(view, "Task deleted", Snackbar.LENGTH_SHORT).setAction("Undo") {
                    viewModel.onUndoClick(attribute)
                }
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

    fun getDurationFromLong(duration: Long?): String? {
        return if (duration != null) {
            return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(duration)
            /*val hours: Int = (duration / 3600_000).toInt()
            val minutes: Int = (duration / 60_000 % 60).toInt()
            val minutesToString = if (minutes < 10) "0${minutes}" else minutes.toString()
            if (hours < 1)
                minutesToString + "min"
            else
                "${hours}h$minutesToString"*/
        } else null
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
        binding.progressText.text = "Progress: $progress %"

        //if (progress == 100F)
        //make a sound
    }

}