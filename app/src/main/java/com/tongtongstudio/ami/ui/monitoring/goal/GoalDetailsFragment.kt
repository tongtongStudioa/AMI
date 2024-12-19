package com.tongtongstudio.ami.ui.monitoring.goal

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.FragmentGoalDetailsBinding
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GoalDetailsFragment : Fragment(R.layout.fragment_goal_details) {
    lateinit var binding: FragmentGoalDetailsBinding
    private val viewModel: GoalDetailsViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGoalDetailsBinding.bind(view)
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp(appBarConfiguration)
        }
    }
}