package com.tongtongstudio.ami.ui.completed

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ThingToDoItemCallback
import com.tongtongstudio.ami.adapter.task.InteractionListener
import com.tongtongstudio.ami.adapter.task.ThingToDoAdapter
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CompletedThingToDoFragment : Fragment(R.layout.fragment_main),
    InteractionListener {

    private val viewModel: CompletedThingToDoViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var sharedViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        // to make toolbar appear
        setUpToolbar()
        binding.fabAddTask.isVisible = false

        sharedViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        val completedAdapter = ThingToDoAdapter(this)

        binding.apply {
            mainRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = completedAdapter
            }

            val callback = object : ThingToDoItemCallback(
                completedAdapter,
                ItemTouchHelper.RIGHT,
                requireContext()
            ) {
                override fun actionOnRightSwiped(thingToDo: ThingToDo) {
                    sharedViewModel.deleteTask(thingToDo, requireContext())
                }

            }
            ItemTouchHelper(callback).attachToRecyclerView(mainRecyclerView)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.mainEvent.collect { event ->
                    when (event) {
                        is MainViewModel.SharedEvent.ShowUndoDeleteTaskMessage -> {
                            Snackbar.make(
                                requireView(),
                                getString(R.string.msg_thing_to_do_deleted),
                                Snackbar.LENGTH_LONG
                            )
                                .setAction(getString(R.string.msg_action_undo)) {
                                    sharedViewModel.onUndoDeleteClick(event.thingToDo)
                                }.show()
                        }

                        is MainViewModel.SharedEvent.NavigateToTaskDetailsScreen -> {
                            val action =
                                CompletedThingToDoFragmentDirections.actionCompletedThingToDoFragmentToDetailsFragment(
                                    event.task
                                )
                            val extras =
                                FragmentNavigatorExtras(event.sharedView to event.sharedView.transitionName)
                            exitTransition = MaterialElevationScale(false).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition = MaterialElevationScale(true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            findNavController().navigate(action, extras)
                        }

                        is MainViewModel.SharedEvent.NavigateToLocalProjectStatsScreen -> {
                            val action =
                                CompletedThingToDoFragmentDirections.actionCompletedThingToDoFragmentToLocalProjectStatsFragment2(
                                    event.composedTaskData
                                )
                            findNavController().navigate(action)
                        }

                        else -> {
                            //do nothing
                        }
                    }
                }
            }
        }

        /**
         * The below code is required to animate correctly when the user returns from [TaskDetailsFragment].
         */
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        viewModel.thingsToDoCompleted.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text =
                    getString(R.string.text_explication_no_tasks_completed)
                binding.emptyRecyclerView.textViewActionText.text =
                    getString(R.string.text_action_no_tasks_completed)
                binding.textSup.text = getString(R.string.nb_job_completed_info, 0)
            } else {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
                completedAdapter.submitList(it)
                binding.textSup.text = getString(R.string.nb_job_completed_info, it.size)
            }
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
        binding.collapseToolbar.setupWithNavController(
            binding.toolbar,
            navController,
            appBarConfiguration
        )
        binding.toolbar.setNavigationOnClickListener {
            exitTransition = MaterialFadeThrough().apply {
                duration = resources.getInteger(R.integer.middle_duration).toLong()
            }
            navController.navigateUp(appBarConfiguration)
        }
        binding.toolbar.subtitle = getString(R.string.completed_tasks_subtitle)
    }

    override fun onTaskChecked(thingToDo: ThingToDo, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
    }

    override fun onComposedTaskClick(thingToDo: ThingToDo) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: ThingToDo, itemView: View) {
        sharedViewModel.navigateToTaskDetailsScreen(thingToDo, itemView)
    }

    override fun onProjectAddClick(composedTask: ThingToDo) {
        // do nothing
    }

    override fun onSubTaskRightSwipe(thingToDo: ThingToDo) {
        sharedViewModel.deleteSubTask(thingToDo)
    }

    override fun onSubTaskLeftSwipe(thingToDo: ThingToDo) {
        sharedViewModel.updateSubTask(thingToDo)
    }

}