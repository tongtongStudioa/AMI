package com.tongtongstudio.ami.ui.otherstasks

import android.graphics.Canvas
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
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
import com.tongtongstudio.ami.adapter.task.InteractionListener
import com.tongtongstudio.ami.adapter.task.TaskAdapter
import com.tongtongstudio.ami.data.LaterFilter
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

@AndroidEntryPoint
class OthersTasksFragment : Fragment(R.layout.fragment_main), InteractionListener {

    private val viewModel: OthersTasksViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var mainAdapter: TaskAdapter
    private lateinit var textExplication: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        // set up the toolbar
        setUpToolbar()

        // empty text view explication
        textExplication = getString(R.string.text_explication_no_tasks_later)

        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        mainAdapter = TaskAdapter(this)

        binding.apply {
            mainRecyclerView.apply {
                adapter = mainAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            fabAddTask.setOnClickListener {
                sharedViewModel.addThingToDo()
            }

            mainRecyclerView.apply {
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
                    val thingToDo = mainAdapter.getTaskList()[viewHolder.absoluteAdapterPosition]
                    if (direction == ItemTouchHelper.RIGHT) {
                        sharedViewModel.deleteTask(thingToDo)
                    } else if (direction == ItemTouchHelper.LEFT) {
                        sharedViewModel.updateTask(thingToDo)
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

        viewModel.preferencesLiveData.observe(viewLifecycleOwner) {
            viewModel.laterFilter = it.filter
        }
        viewModel.otherThingsToDo.observe(viewLifecycleOwner) {
            updateTextExplication(viewModel.laterFilter, it.size)
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text = textExplication
                binding.emptyRecyclerView.textViewActionText.text =
                    getString(R.string.text_action_no_tasks)
            } else {
                mainAdapter.submitList(it)
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
            }
        }

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            sharedViewModel.mainEvent.collect { event ->
                when (event) {
                    is MainViewModel.SharedEvent.NavigateToEditScreen -> {
                        val action =
                            OthersTasksFragmentDirections.actionOthersTasksFragmentToAddEditTaskFragment(
                                getString(R.string.fragment_title_edit_thing_to_do),
                                event.thingToDo
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.NavigateToAddScreen -> {
                        val action =
                            OthersTasksFragmentDirections.actionOthersTasksFragmentToAddEditTaskFragment(
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
                            OthersTasksFragmentDirections.actionOthersTasksFragmentToDetailsFragment(
                                event.task
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.NavigateToLocalProjectStatsScreen -> {
                        val action =
                            OthersTasksFragmentDirections.actionOthersTasksFragmentToLocalProjectStatsFragment2(
                                event.composedTaskData
                            )
                        findNavController().navigate(action)
                    }
                    else -> {}
                }.exhaustive
            }
        }

        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.later_tasks_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_filter_for_tomorrow -> {
                        textExplication = getString(R.string.text_explication_no_tasks_tomorrow)
                        sharedViewModel.onLaterFilterSelected(LaterFilter.TOMORROW)
                        true
                    }
                    R.id.action_filter_for_next_week -> {
                        textExplication = getString(R.string.text_explication_no_tasks_next_week)
                        sharedViewModel.onLaterFilterSelected(LaterFilter.NEXT_WEEK)
                        true
                    }
                    R.id.action_filter_all_later_things -> {
                        textExplication = getString(R.string.text_explication_no_tasks_later)
                        sharedViewModel.onLaterFilterSelected(LaterFilter.LATER)
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
            navController.navigateUp(appBarConfiguration)
        }
    }

    private fun updateTextExplication(laterFilter: LaterFilter?, tasksCount: Int) {
        when (laterFilter) {
            LaterFilter.TOMORROW -> {
                binding.textSup.text =
                    getString(R.string.nb_future_tasks_tomorrow, tasksCount)
            }
            LaterFilter.NEXT_WEEK -> binding.textSup.text =
                getString(R.string.nb_future_tasks_nex_week, tasksCount)
            LaterFilter.LATER -> binding.textSup.text =
                getString(R.string.nb_future_tasks_later, tasksCount)
            else -> {}//do nothing
        }
    }

    override fun onTaskChecked(thingToDo: Task, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
    }

    override fun onComposedTaskClick(thingToDo: ThingToDo) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: Task) {
        sharedViewModel.navigateToTaskDetailsScreen(thingToDo)
    }

    override fun onProjectAddClick(composedTask: ThingToDo) {
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to composedTask.mainTask.id))
        sharedViewModel.addThingToDo()
    }

    override fun onSubTaskRightSwipe(thingToDo: Task) {
        //TODO("Not yet implemented")
    }

    override fun onSubTaskLeftSwipe(thingToDo: Task) {
        //TODO("Not yet implemented")
    }
}