package com.tongtongstudio.ami.ui.monitoring.task

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialContainerTransform
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.FragmentViewPagerBinding
import com.tongtongstudio.ami.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ViewPagerTrackingAndStatsFragment : Fragment(R.layout.fragment_view_pager) {

    private val viewModel: TaskDetailsAndTimeTrackerViewModel by viewModels()
    lateinit var binding: FragmentViewPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = resources.getInteger(R.integer.long_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentViewPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpToolbar()
        binding.apply {
            viewPager2.adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
            viewPager2.setCurrentItem(viewModel.fragmentPos, true)
            viewPager2.offscreenPageLimit = 2 // Preload two fragments for smoother transitions
        }

        postponeEnterTransition()
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    val childFragment =
                        childFragmentManager.findFragmentByTag("f0") as? TaskDetailsFragment
                    childFragment?.let {
                        startPostponedEnterTransition()
                    }
                }
            }
        })

        TabLayoutMediator(
            binding.tabLayout, binding.viewPager2
        ) { tab: TabLayout.Tab, position: Int ->
            tab.text = when (position) {
                0 -> getString(R.string.fragment_task_details_title)
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