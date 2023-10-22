package com.tongtongstudio.ami.ui.monitoring.projectStats

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.ProjectStatsFragmentBinding
import com.tongtongstudio.ami.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocalProjectStatsFragment : Fragment(R.layout.project_stats_fragment) {
    lateinit var binding: ProjectStatsFragmentBinding
    private val viewModel: ProjectStatsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ProjectStatsFragmentBinding.bind(view)

        setUpToolbar()

        // TODO: change this page with placeholders
        binding.estimatedTime.text = getDurationFromLong(viewModel.estimatedTime) ?: getText(R.string.no_information)
        binding.totalWorkTime.text = getDurationFromLong(viewModel.workTime) ?: getText(R.string.no_information)
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
            val hours: Int = (duration / 3600_000).toInt()
            val minutes: Int = (duration / 60_000 % 60).toInt()
            val minutesToString = if (minutes < 10) "0${minutes}" else minutes.toString()
            if (hours < 1)
                minutesToString + "min"
            else
                "${hours}h$minutesToString"
        } else null
    }
}