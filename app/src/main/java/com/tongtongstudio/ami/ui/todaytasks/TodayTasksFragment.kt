package com.tongtongstudio.ami.ui.todaytasks

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
import com.tongtongstudio.ami.adapter.InteractionListener
import com.tongtongstudio.ami.adapter.TaskAdapter
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.ui.todaytasks.TodayTasksFragmentDirections.Companion.actionTodayTasksFragmentToAddEditTaskFragment
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat


@AndroidEntryPoint
class TodayTasksFragment : Fragment(R.layout.fragment_main), InteractionListener {

    private val viewModel: TasksViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var newTaskAdapter: TaskAdapter
    private lateinit var sharedViewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMainBinding.bind(view)

        //collapse toolbar
        setUpToolbar()
        //view model and adapter
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        newTaskAdapter = TaskAdapter(this)

        // implement UI
        binding.apply {
            fabAddTask.setOnClickListener {
                sharedViewModel.onAddThingToDoDemand()
            }

            mainRecyclerView.apply {
                adapter = newTaskAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
            }
            /*val callback = TaskTouchHelperCallback(newTaskAdapter,
                { newSubTask, parentId ->
                    // TODO: use drag and drop to add subTTask
                //sharedViewModel.addSubTask(newSubTask,parentId)
            },
                { taskWithSubTasks ->
                    sharedViewModel.onThingToDoRightSwiped(taskWithSubTasks)
                },
                { taskWithSubTasks ->
                    sharedViewModel.onThingToDoLeftSwiped(taskWithSubTasks)
                })*/
            //ItemTouchHelper(callback).attachToRecyclerView(mainRecyclerView)

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
                    val thingToDo = newTaskAdapter.getTaskList()[viewHolder.absoluteAdapterPosition]
                    if (direction == ItemTouchHelper.RIGHT) {
                        // delete task
                        newTaskAdapter.notifyItemRemoved(viewHolder.absoluteAdapterPosition)
                        sharedViewModel.onThingToDoRightSwiped(thingToDo)
                    } else if (direction == ItemTouchHelper.LEFT) {
                        // edit task
                        newTaskAdapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)
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

        // listen to request's result
        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            sharedViewModel.onAddEditResult(
                result,
                resources.getStringArray(R.array.thing_to_do_added),
                resources.getStringArray(R.array.thing_to_do_updated)
            )
        }

        // adapt data in recycler view
        viewModel.todayThingsToDo.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text =
                    getText(R.string.text_explication_no_tasks_today)
                val upcomingTasksCount = viewModel.getUpcomingTtdCount()
                binding.emptyRecyclerView.textViewActionText.text =
                    getString(R.string.text_action_no_tasks_today, upcomingTasksCount)
                binding.toolbar.collapseActionView()
            } else {
                newTaskAdapter.submitList(it)
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
            }
        }

        // respond to event
        // TODO: change this with the good life cycle state : if not all event respond to that collect method
        /*lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {*/
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            sharedViewModel.mainEvent.collect { event ->
                when (event) {
                    is MainViewModel.SharedEvent.NavigateToEditScreen -> {
                        val action =
                            actionTodayTasksFragmentToAddEditTaskFragment(
                                getString(R.string.fragment_title_edit_thing_to_do),
                                event.thingToDo
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.NavigateToAddScreen -> {
                        val action =
                            actionTodayTasksFragmentToAddEditTaskFragment(
                                getString(R.string.fragment_title_add_thing_to_do),
                                null
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.ShowTaskSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    is MainViewModel.SharedEvent.ShowUndoDeleteTaskMessage -> {
                        Snackbar.make(
                            requireView(),
                            event.textShow,
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getText(R.string.msg_action_undo)) {
                                sharedViewModel.onUndoDeleteClick(event.thingToDo)
                            }.show()
                    }
                    is MainViewModel.SharedEvent.NavigateToTrackingScreen -> {
                        val action =
                            TodayTasksFragmentDirections.actionTodayTasksFragmentToChronometerFragment(
                                event.task
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.NavigateToLocalProjectStatsScreen -> {
                        val action =
                            TodayTasksFragmentDirections.actionTodayTasksFragmentToLocalProjectStatsFragment2(
                                event.composedTaskData
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.ShowMissedRecurringTaskDialog -> {
                        val action =
                            TodayTasksFragmentDirections.actionTodayTasksFragmentToMissedRecurringTasksDialogFragment(
                                event.missedTasks.toTypedArray()
                            )
                        findNavController().navigate(action)
                    }
                }.exhaustive
            }
        }

        // add menu
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
            R.id.action_sort_by_name -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_NAME)
                true
            }

            R.id.action_sort_by_eisenhower_matrix -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_EISENHOWER_MATRIX)
                true
            }

            R.id.action_sort_by_2_minutes_rules -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_2MINUTES_RULES)
                true
            }

            R.id.action_sort_by_deadline -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_DEADLINE)
                true
            }

            R.id.action_sort_by_creator_sort -> {
                sharedViewModel.onSortOrderSelected(SortOrder.BY_CREATOR_SORT)
                true
            }

            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                sharedViewModel.onHideCompletedClick(item.isChecked)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // function for adapter (today's tasks)
    /*override fun onCheckBoxClick(task: Task, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(task, isChecked)
        if (isChecked) {
            sharedViewModel.soundPool.load(this.context, R.raw.success_2, 1)
            sharedViewModel.soundPool.play(1, 0.25F, 0.25F, 0, 0, 1.1F)
        }
    }

    override fun onItemThingToDoClicked(thingToDo: ThingToDo) {
        sharedViewModel.onThingToDoClicked(thingToDo)
    }

    override fun onItemTaskSwiped(subTask: Task, dir: Int) {
        if (dir == ItemTouchHelper.RIGHT)
            sharedViewModel.onSubTaskRightSwiped(subTask)
        else sharedViewModel.onSubTaskLeftSwiped(subTask)
    }

    override fun onProjectBtnAddSubTaskClicked(projectData: ProjectWithSubTasks) {
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to projectData.project.p_id))
        sharedViewModel.onAddThingToDoDemand()
    }*/

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
        binding.toolbar.subtitle = "Today's things to do"
        binding.textSup.text =
            SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(viewModel.startOfToday)
    }

    override fun onTaskChecked(thingToDo: Ttd, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
        if (isChecked) {
            sharedViewModel.soundPool.load(this.context, R.raw.success_2, 1)
            sharedViewModel.soundPool.play(1, 0.25F, 0.25F, 0, 0, 1.1F)
        }
    }

    override fun onComposedTaskClick(thingToDo: TaskWithSubTasks) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: Ttd) {
        sharedViewModel.navigateToTaskInfoScreen(thingToDo)
    }

    override fun onAddClick(composedTask: TaskWithSubTasks) {
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to composedTask.mainTask.id))
        sharedViewModel.onAddThingToDoDemand()
    }

    override fun onSubTaskRightSwipe(thingToDo: Ttd) {
        //TODO("Not yet implemented")
    }

    override fun onSubTaskLeftSwipe(thingToDo: Ttd) {
        //TODO("Not yet implemented")
    }
}