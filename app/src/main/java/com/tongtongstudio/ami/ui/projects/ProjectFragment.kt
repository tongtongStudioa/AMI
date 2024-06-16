package com.tongtongstudio.ami.ui.projects

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.InteractionListener
import com.tongtongstudio.ami.adapter.TaskAdapter
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.FragmentMainBinding
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
    private lateinit var newTaskAdapter: TaskAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        setUpToolbar()

        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        newTaskAdapter = TaskAdapter(this)

        binding.apply {
            fabAddTask.setOnClickListener {
                sharedViewModel.addThingToDo()
            }

            mainRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = newTaskAdapter
                setHasFixedSize(true)
            }
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
                    newTaskAdapter.submitList(it)
                    emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                    mainRecyclerView.isVisible = true
                    textSup.text = getString(R.string.nb_projects_info, it.size)
                }
            }
        }

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            sharedViewModel.onAddEditResult(
                result,
                resources.getStringArray(R.array.thing_to_do_added),
                resources.getStringArray(R.array.thing_to_do_updated)
            )
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
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.NavigateToAddScreen -> {
                        val action =
                            ProjectFragmentDirections.actionProjectFragmentToAddEditTaskFragment(
                                getString(R.string.fragment_title_add_thing_to_do),
                                null
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.ShowTaskSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
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
                    is MainViewModel.SharedEvent.ShowMissedRecurringTaskDialog -> {
                        // val action
                    }
                    is MainViewModel.SharedEvent.NavigateToTaskDetailsScreen -> {
                        val action =
                            ProjectFragmentDirections.actionProjectFragmentToTabPageTrackingStats(
                                event.task
                            )
                        findNavController().navigate(action)
                    }
                }.exhaustive
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.today_tasks_menu, menu)
        lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.preferencesFlow.first().hideCompleted
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_by_eisenhower_matrix -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_EISENHOWER_MATRIX)
                true
            }
            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                sharedViewModel.onHideCompletedClick(item.isChecked)
                true
            }
            R.id.action_sort_by_name -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_NAME)
                true
            }
            R.id.action_sort_by_deadline -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_DEADLINE)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
            navController.navigateUp(appBarConfiguration)
        }
    }

    override fun onTaskChecked(thingToDo: Ttd, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
    }

    override fun onComposedTaskClick(thingToDo: TaskWithSubTasks) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: Ttd) {
        sharedViewModel.navigateToTaskDetailsScreen(thingToDo)
    }

    override fun onProjectAddClick(composedTask: TaskWithSubTasks) {
        // TODO: create another event for sub task add action which take composed task as argument
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to composedTask.mainTask.id))
        sharedViewModel.addThingToDo()
    }

    override fun onSubTaskRightSwipe(thingToDo: Ttd) {
        sharedViewModel.deleteSubTask(thingToDo)
    }

    override fun onSubTaskLeftSwipe(thingToDo: Ttd) {
        sharedViewModel.updateSubTask(thingToDo)
    }
}