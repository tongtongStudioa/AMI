package com.tongtongstudio.ami.ui.drafts

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
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
import com.tongtongstudio.ami.databinding.FragmentDraftsBinding
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DraftsFragment : Fragment(R.layout.fragment_drafts), InteractionListener {

    private val viewModel: DraftsViewModel by viewModels()
    private lateinit var binding: FragmentDraftsBinding
    private lateinit var sharedViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }
        super.onCreate(savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDraftsBinding.bind(view)

        setUpToolbar()
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val draftAdapter = ThingToDoAdapter(this)
        binding.draftRecyclerView.apply {
            adapter = draftAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
            postponeEnterTransition()
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
        }

        val callback = object : ThingToDoItemCallback(
            draftAdapter,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
            requireContext()
        ) {
            override fun actionOnRightSwiped(thingToDo: ThingToDo) {
                // delete thingToDo
                sharedViewModel.deleteTask(thingToDo, requireContext())
            }

            override fun actionLeftSwiped(thingToDo: ThingToDo) {
                //update thingToDo
                sharedViewModel.updateTask(thingToDo)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.draftRecyclerView)

        viewModel.draftTasks.observe(viewLifecycleOwner) { tasks ->
            draftAdapter.submitList(tasks)
            binding.emptyDraftRecyclerView.viewEmptyRecyclerView.isVisible = tasks.isEmpty()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.mainEvent.collect { event ->
                    when (event) {
                        is MainViewModel.SharedEvent.NavigateToEditScreen -> {
                            val action =
                                DraftsFragmentDirections.actionDraftsFragmentToAddEditTaskFragment(
                                    getString(R.string.fragment_title_edit_thing_to_do),
                                    event.thingToDo
                                )
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToAddScreen -> {
                            val action =
                                DraftsFragmentDirections.actionDraftsFragmentToAddEditTaskFragment(
                                    getString(R.string.fragment_title_add_thing_to_do),
                                    null
                                )
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.ShowConfirmationMessage -> {
                            val msg = if (event.result == ADD_TASK_RESULT_OK)
                                getString(R.string.task_added)
                            else getString(R.string.task_updated)
                            Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
                        }

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
                                DraftsFragmentDirections.actionDraftsFragmentToDetailsFragment(
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
                                DraftsFragmentDirections.actionDraftsFragmentToProjectDetailsFragment(
                                    event.composedTaskData
                                )
                            findNavController().navigate(action)
                        }

                        else -> {}
                    }.exhaustive
                }
            }
        }
    }

    override fun onTaskChecked(thingToDo: ThingToDo, isChecked: Boolean, position: Int) {
        // do nothing
    }

    override fun onComposedTaskClick(thingToDo: ThingToDo) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: ThingToDo, itemView: View) {
        sharedViewModel.navigateToTaskDetailsScreen(thingToDo, itemView)
    }

    override fun onProjectAddClick(composedTask: ThingToDo) {
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to composedTask.mainTask.id))
        sharedViewModel.addThingToDo()
    }

    override fun onSubTaskRightSwipe(thingToDo: ThingToDo) {
        sharedViewModel.deleteSubTask(thingToDo)
    }

    override fun onSubTaskLeftSwipe(thingToDo: ThingToDo) {
        sharedViewModel.updateSubTask(thingToDo)
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
            exitTransition = MaterialFadeThrough().apply {
                duration = resources.getInteger(R.integer.middle_duration).toLong()
            }
            navController.navigateUp(appBarConfiguration)
        }
    }
}