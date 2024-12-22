package com.tongtongstudio.ami.ui.monitoring.goal

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialContainerTransform
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.FragmentGoalDetailsBinding
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat

@AndroidEntryPoint
class GoalDetailsFragment : Fragment(R.layout.fragment_goal_details) {
    lateinit var binding: FragmentGoalDetailsBinding
    private val viewModel: GoalDetailsViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGoalDetailsBinding.bind(view)
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.long_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
        // Shared transition id
        ViewCompat.setTransitionName(binding.goalCardView, "shared_element_${viewModel.goal?.id}")

        binding.apply {
            goalName.text = viewModel.goal?.title ?: getString(R.string.no_information)
            goalCategory.text = viewModel.category?.title
            goalCategory.isVisible = viewModel.category != null
            goalDescription.text = viewModel.goal?.description
            goalDescription.isVisible = viewModel.goal?.description != null
            goalDueDate.text = if (viewModel.goal?.dueDate != null)
                DateFormat.getDateInstance().format(viewModel.goal?.dueDate)
            else getString(R.string.no_information)
            tvGoal.text = viewModel.goal?.targetGoal.toString()
            val evaluationsAdapter = EditAttributesAdapter(object : AttributeListener<Assessment> {
                override fun onItemClicked(attribute: Assessment) {
                    TODO("Not yet implemented")
                }

                override fun onRemoveCrossClick(attribute: Assessment) {
                    TODO("Not yet implemented")
                }

            }) { binding, assessment ->
                binding.titleOverview.text = assessment.title
            }

            rvEvaluations.apply {
                adapter = evaluationsAdapter
                layoutManager = LinearLayoutManager(context)
            }
            viewModel.intermediateEvaluations?.observe(viewLifecycleOwner) {
                evaluationsAdapter.submitList(it)
            }
            // TODO: show graph with evaluations
        }

        val mainActivity = activity as MainActivity
        // imperative to see option menu and navigation icon (hamburger)
        mainActivity.setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp(mainActivity.appBarConfiguration)
        }
    }


}