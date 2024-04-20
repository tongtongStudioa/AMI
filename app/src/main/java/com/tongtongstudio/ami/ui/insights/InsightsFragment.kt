package com.tongtongstudio.ami.ui.insights

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.FragmentInsightsBinding
import com.tongtongstudio.ami.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InsightsFragment : Fragment(R.layout.fragment_insights) {

    private val viewModel: InsightsViewModel by viewModels()
    private lateinit var binding: FragmentInsightsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInsightsBinding.bind(view)

        setUpToolbar()

        viewModel.tasksCompletedLD.observe(viewLifecycleOwner) {
            val nbTaskCompleted = it.size
            val taskAverageTimeCompletion = viewModel.getAverageTimeCompletion(it)
            val estimationAccuracyRate = viewModel.retrieveEstimationWorkTimeAccuracyRate(it)
            val onTimeCompletionRate = viewModel.retrieveOnTimeCompletionRate(it)

            displayInsightsOnTasksCompleted(
                nbTaskCompleted,
                taskAverageTimeCompletion,
                estimationAccuracyRate,
                onTimeCompletionRate
            )
        }

        viewModel.projectsLD.observe(viewLifecycleOwner) {
            val nbProjectsCompleted = viewModel.getProjectsCompleted(it)
            val projectsAchievementRate = viewModel.getProjectAchievementRate(it)
            displayInsightsOnProjects(nbProjectsCompleted, projectsAchievementRate)
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

    // TODO: 23/03/2023 save nb tasks completed in database to save completed tasks deleted
    //  and easiest way to retrieve this data
    //  and this list is only tasks list !!
    private fun displayInsightsOnTasksCompleted(
        nbTaskCompleted: Int,
        taskAverageTimeCompletion: Long?,
        estimationAccuracyRate: Float?,
        onTimeCompletionRate: Float?
    ) {
        binding.tvNbTasksCompleted.text = nbTaskCompleted.toString()
        binding.tvAverageTimeCompletion.text =
            if (taskAverageTimeCompletion != null) { // todo: by categories
                val hours: Int = (taskAverageTimeCompletion / 3600_000).toInt()
                val minutes: Int = (taskAverageTimeCompletion / 60_000 % 60).toInt()
                val minutesToString = if (minutes < 10) "0${minutes}" else minutes.toString()
                "${hours}h$minutesToString"
            } else getString(R.string.no_information)
        binding.tvEstimationTimeAccuracy.text =
            if (estimationAccuracyRate != null) "${estimationAccuracyRate % .1F}%" else getString(R.string.no_information)
        binding.tvOnRateCompletionTime.text =
            if (onTimeCompletionRate != null) "${onTimeCompletionRate % .1F}%" else getString(R.string.no_information)
        binding.tvBestStreak.text = getString(R.string.no_information)
    }

    private fun displayInsightsOnProjects(
        nbProjectsCompleted: Int,
        projectsAchievementRate: Float?
    ) {
        binding.tvProjectsCompleted.text =
            nbProjectsCompleted.toString()
        binding.tvProjectsAchievementRate.text = if (projectsAchievementRate != null)
            "${projectsAchievementRate % .1F}%"
        else getString(R.string.no_information)
    }
}