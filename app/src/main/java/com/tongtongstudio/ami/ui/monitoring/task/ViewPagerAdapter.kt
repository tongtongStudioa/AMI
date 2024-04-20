package com.tongtongstudio.ami.ui.monitoring.task

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    private val mFragments = arrayOf<Fragment>( //Initialize fragments views
        //Fragment views are initialized like any other fragment (Extending Fragment)
        TaskDetailsFragment(),
        TimeTrackerFragment()
    )

    override fun getItemCount(): Int {
        return mFragments.size //Number of fragments displayed
    }

    override fun createFragment(position: Int): Fragment {
        return mFragments[position]
    }
}