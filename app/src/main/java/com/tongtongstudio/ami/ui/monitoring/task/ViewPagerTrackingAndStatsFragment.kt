package com.tongtongstudio.ami.ui.monitoring.task

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.FragmentViewPagerBinding
import com.tongtongstudio.ami.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ViewPagerTrackingAndStatsFragment : Fragment(R.layout.fragment_view_pager) {

    private val viewModel: TaskDetailsAndTimeTrackerViewModel by viewModels()
    lateinit var binding: FragmentViewPagerBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentViewPagerBinding.bind(view)

        //Log.d(javaClass.simpleName, "safe args received: ${viewModel.task}")
        setUpToolbar()

        binding.apply {
            viewPager2.adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
            viewPager2.setCurrentItem(viewModel.fragmentPos, true)
        }
        TabLayoutMediator(
            binding.tabLayout, binding.viewPager2
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = when (position) {
                0 -> getString(R.string.task_details_screen_title)
                1 -> getString(R.string.fragment_title_chronometer)
                else -> "no name lol"
            }
        }.attach()
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