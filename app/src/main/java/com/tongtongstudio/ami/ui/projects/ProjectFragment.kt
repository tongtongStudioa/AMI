package com.tongtongstudio.ami.ui.projects

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ThingToDoItemCallback
import com.tongtongstudio.ami.adapter.task.InteractionListener
import com.tongtongstudio.ami.adapter.task.ThingToDoAdapter
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
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var binding: FragmentMainBinding
    private lateinit var mainTaskAdapter: ThingToDoAdapter

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
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainTaskAdapter = ThingToDoAdapter(this)

        binding.apply {
            fabAddTask.setOnClickListener {
                sharedViewModel.addThingToDo()
            }

            mainRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = mainTaskAdapter
                setHasFixedSize(true)
            }
            val callback = object : ThingToDoItemCallback(
                mainTaskAdapter,
                ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
                requireContext()
            ) {
                override fun actionOnRightSwiped(thingToDo: ThingToDo) {
                    // delete task
                    sharedViewModel.deleteTask(thingToDo, requireContext())
                }

                override fun actionLeftSwiped(thingToDo: ThingToDo) {
                    //update task
                    sharedViewModel.updateTask(thingToDo)
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
                    mainTaskAdapter.submitList(it)
                    emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                    mainRecyclerView.isVisible = true
                    //textSup.text = getString(R.string.nb_projects_info, it.size)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
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
                        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                            duration = resources.getInteger(R.integer.middle_duration).toLong()
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
                        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
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
                        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
                    }
                    is MainViewModel.SharedEvent.NavigateToTaskViewPager -> {
                        val action =
                            ProjectFragmentDirections.actionProjectFragmentToTabPageTrackingStats(
                                event.task
                            )
                        findNavController().navigate(action)
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
                    is MainViewModel.SharedEvent.NavigateToLocalProjectStatsScreen -> {
                        val action =
                            ProjectFragmentDirections.actionProjectFragmentToLocalProjectStatsFragment2(
                                event.composedTaskData
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.NavigateToTaskDetailsScreen -> {
                        val action =
                            ProjectFragmentDirections.actionProjectFragmentToTabPageTrackingStats(
                                event.task
                            )
                        findNavController().navigate(action)
                    }
                    else -> {
                        // do nothing
                    }
                }.exhaustive
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

    override fun onTaskChecked(thingToDo: Task, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
    }

    override fun onComposedTaskClick(thingToDo: ThingToDo) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: Task, itemView: View) {
        sharedViewModel.navigateToTaskDetailsScreen(thingToDo, itemView)
    }

    override fun onProjectAddClick(composedTask: ThingToDo) {
        // TODO: create another event for sub task add action which take composed task as argument
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to composedTask.mainTask.id))
        sharedViewModel.addThingToDo()
    }

    override fun onSubTaskRightSwipe(thingToDo: Task) {
        sharedViewModel.deleteSubTask(thingToDo)
    }

    override fun onSubTaskLeftSwipe(thingToDo: Task) {
        sharedViewModel.updateSubTask(thingToDo)
    }
}