package com.tongtongstudio.ami.ui.projects

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ThingToDoItemCallback
import com.tongtongstudio.ami.adapter.project.ProjectAdapter
import com.tongtongstudio.ami.adapter.thingToDo.InteractionListener
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.ADD_DRAFT_TASK_OK
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProjectFragment : Fragment(R.layout.fragment_main), InteractionListener {

    private val viewModel: ProjectViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var projectAdapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }
        super.onCreate(savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        setUpToolbar()
        projectAdapter = ProjectAdapter(this)

        binding.apply {
            fabAddTask.setOnClickListener {
                sharedViewModel.addThingToDo()
            }

            mainRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = projectAdapter
                setHasFixedSize(true)
            }
            val callback = object : ThingToDoItemCallback<ProjectAdapter>(
                projectAdapter,
                ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
                requireContext()
            ) {
                 override fun actionOnRightSwiped(thingToDo: ThingToDo, position: Int) {
                     // delete task
                     sharedViewModel.deleteTask(thingToDo, requireContext())
                     projectAdapter.notifyItemRemoved(position)

                 }

                override fun actionLeftSwiped(thingToDo: ThingToDo, position: Int) {
                    //update task
                    sharedViewModel.updateTask(thingToDo)
                    projectAdapter.notifyItemChanged(position)
                }
            }
            ItemTouchHelper(callback).attachToRecyclerView(mainRecyclerView)
        }

        viewModel.projects.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text =
                    getString(R.string.text_explication_no_projects)
                binding.emptyRecyclerView.textViewActionText.text =
                    getString(R.string.text_action_no_projects)
            } else {
                binding.apply {
                    projectAdapter.submitList(it)
                    emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                    mainRecyclerView.isVisible = true
                    //textSup.text = getString(R.string.nb_projects_info, it.size)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.mainEvent.collect { event ->
                    when (event) {
                        is MainViewModel.SharedEvent.NavigateToEditScreen -> {
                            val action =
                                ProjectFragmentDirections.actionProjectFragmentToAddEditTaskFragment(
                                    getString(R.string.fragment_title_edit_thing_to_do),
                                    event.thingToDo

                                )
                            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition =
                                MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                                    duration =
                                        resources.getInteger(R.integer.middle_duration).toLong()
                                }
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToAddScreen -> {
                            val action =
                                ProjectFragmentDirections.actionProjectFragmentToAddEditTaskFragment(
                                    getString(R.string.fragment_title_add_thing_to_do),
                                    null
                                )
                            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition =
                                MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                                    duration =
                                        resources.getInteger(R.integer.middle_duration).toLong()
                                }
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToAddScreenWithParentTask -> {
                            val action = ProjectFragmentDirections.actionProjectFragmentToAddEditTaskFragment(
                                title = getString(R.string.fragment_title_add_thing_to_do),
                                thingToDo = null,
                                parentTask = event.parentTask
                            )
                            exitTransition = MaterialElevationScale(false).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition = MaterialElevationScale(true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.ShowConfirmationMessage -> {
                            val msg = when (event.result) {
                                ADD_TASK_RESULT_OK -> getString(R.string.task_added)
                                ADD_DRAFT_TASK_OK -> getString(R.string.draft_task_created)
                                else -> getString(R.string.task_updated)
                            }
                            Snackbar.make(binding.fabAddTask, msg, Snackbar.LENGTH_SHORT)
                                .setAnchorView(binding.fabAddTask)
                                .show()
                        }

                        is MainViewModel.SharedEvent.ShowUndoDeleteTaskMessage -> {
                            Snackbar.make(
                                binding.fabAddTask,
                                getString(R.string.msg_thing_to_do_deleted),
                                Snackbar.LENGTH_LONG
                            )
                                .setAnchorView(binding.fabAddTask)
                                .setAction(getString(R.string.msg_action_undo)) {
                                    sharedViewModel.onUndoDeleteClick(event.thingToDo)
                                }.show()
                        }

                        is MainViewModel.SharedEvent.NavigateToProjectDetailsScreen -> {
                            val action =
                                ProjectFragmentDirections.actionProjectFragmentToLocalProjectStatsFragment2(
                                    event.project.taskRelations.mainTask.id
                                )
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToTaskDetailsScreen -> {
                            val action =
                                ProjectFragmentDirections.actionProjectFragmentToTabPageTrackingStats(
                                    event.task.id
                                )
                            findNavController().navigate(action)
                        }

                        else -> {
                            // do nothing
                        }
                    }.exhaustive
                }
            }
        }

        /**
         * The below code is required to animate correctly when the user returns from [TaskDetailsFragment].
         */
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.today_tasks_menu, menu)
                lifecycleScope.launch {
                    menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                        viewModel.preferencesFlow.first().hideCompleted
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_sort_by_eisenhower_matrix -> {
                        sharedViewModel.onSortOrderSelected(SortOrder.BY_EISENHOWER_MATRIX)
                        true
                    }

                    R.id.action_sort_by_2_minutes_rules -> {
                        sharedViewModel.onSortOrderSelected(SortOrder.BY_2MINUTES_RULES)
                        true
                    }

                    R.id.action_sort_by_eat_the_frog -> {
                        sharedViewModel.onSortOrderSelected(SortOrder.BY_EAT_THE_FROG)
                        true
                    }

                    R.id.action_sort_by_creator_sort -> {
                        sharedViewModel.onSortOrderSelected(SortOrder.BY_CREATOR_SORT)
                        true
                    }

                    R.id.action_hide_completed_tasks -> {
                        menuItem.isChecked = !menuItem.isChecked
                        sharedViewModel.onHideCompletedClick(menuItem.isChecked)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)
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
    }

    override fun onTaskChecked(thingToDo: ThingToDo, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
    }

    override fun onProjectClick(thingToDo: ThingToDo, itemView: View, position: Int) {
        sharedViewModel.navigateToProjectDetailsScreen(thingToDo,itemView)
    }

    override fun onTaskClick(thingToDo: Task, itemView: View, position: Int) {
        sharedViewModel.navigateToTaskDetailsScreen(thingToDo, itemView)
    }

    override fun onProjectAddClick(thingToDo: ThingToDo) {
        sharedViewModel.addSubThingTodo(thingToDo.taskRelations.mainTask)
    }
}