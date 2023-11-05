package com.tongtongstudio.ami.ui.projects

import android.graphics.Canvas
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.MainAdapter
import com.tongtongstudio.ami.adapter.ThingToDoListener
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.ProjectWithSubTasks
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.todolistami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProjectFragment : Fragment(R.layout.fragment_main), ThingToDoListener {

    private val viewModel: ProjectViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var binding: FragmentMainBinding
    private lateinit var mainAdapter: MainAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        setUpToolbar()

        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        mainAdapter = MainAdapter(this, requireContext())

        binding.apply {
            fabAddTask.setOnClickListener {
                sharedViewModel.onAddThingToDoDemand()
            }

            mainRecyclerView.apply {
                adapter = mainAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val thingToDo = mainAdapter.data[viewHolder.adapterPosition]
                    if (direction == ItemTouchHelper.RIGHT) {
                        sharedViewModel.onThingToDoRightSwiped(thingToDo)
                    } else if (direction == ItemTouchHelper.LEFT) {
                        sharedViewModel.onThingToDoLeftSwiped(thingToDo)
                    }
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    RecyclerViewSwipeDecorator.Builder(
                        context,
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                        .addSwipeLeftActionIcon(R.drawable.ic_baseline_edit_24)
                        .addSwipeRightActionIcon(R.drawable.ic_baseline_delete_24)
                        .setSwipeLeftActionIconTint(resources.getColor(R.color.md_theme_light_tertiary))
                        .setSwipeRightActionIconTint(resources.getColor(R.color.md_theme_light_error))
                        .create()
                        .decorate()
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }

            }).attachToRecyclerView(mainRecyclerView)
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
                mainAdapter.swapData(it)
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
                binding.textSup.text = getString(R.string.nb_projects_info, it.size)
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
                    is MainViewModel.SharedEvent.NavigateToTrackingScreen -> {
                        val action =
                            ProjectFragmentDirections.actionProjectFragmentToChronometerFragment(
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
                                event.projectData
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.ShowMissedRecurringTaskDialog -> {
                        // val action
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
            R.id.action_sort_by_importance_priority -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_IMPORTANCE_PRIORITY)
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

    override fun onItemThingToDoClicked(thingToDo: ThingToDo) {
        sharedViewModel.onThingToDoClicked(thingToDo)
    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(task, isChecked)
    }

    override fun onItemTaskSwiped(subTask: Task, dir: Int) {
        if (dir == ItemTouchHelper.RIGHT)
            sharedViewModel.onSubTaskRightSwiped(subTask)
        else sharedViewModel.onSubTaskLeftSwiped(subTask)
    }

    override fun onProjectBtnAddSubTaskClicked(projectData: ProjectWithSubTasks) {
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to projectData.project.p_id))
        sharedViewModel.onAddThingToDoDemand()
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
}