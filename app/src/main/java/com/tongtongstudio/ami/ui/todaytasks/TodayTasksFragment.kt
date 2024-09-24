package com.tongtongstudio.ami.ui.todaytasks

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
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ThingToDoItemCallback
import com.tongtongstudio.ami.adapter.task.InteractionListener
import com.tongtongstudio.ami.adapter.task.ThingToDoAdapter
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.notification.SoundPlayer
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.ui.todaytasks.TodayTasksFragmentDirections.Companion.actionTodayTasksFragmentToAddEditTaskFragment
import com.tongtongstudio.ami.ui.todaytasks.TodayTasksFragmentDirections.Companion.actionTodayTasksFragmentToTabPageTrackingStats
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat


@AndroidEntryPoint
class TodayTasksFragment : Fragment(R.layout.fragment_main), InteractionListener {

    private val viewModel: TasksViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var mainTaskAdapter: ThingToDoAdapter
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var soundPlayer: SoundPlayer
    private var menuProvider: MenuProvider? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMainBinding.bind(view)

        //collapse toolbar
        setUpToolbar()

        //view model, sound player and adapter
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainTaskAdapter = ThingToDoAdapter(this)
        soundPlayer = SoundPlayer(requireContext())

        // implement UI
        binding.apply {
            fabAddTask.setOnClickListener {
                sharedViewModel.addThingToDo()
            }

            mainRecyclerView.apply {
                adapter = mainTaskAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
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
        // adapt data in recycler view
        viewModel.todayThingsToDo.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text =
                    getText(R.string.text_explication_no_tasks_today)
                binding.toolbar.collapseActionView()
            } else {
                mainTaskAdapter.submitList(it)
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
            }
        }

        viewModel.upcomingTasksCount.observe(viewLifecycleOwner) {
            binding.emptyRecyclerView.textViewActionText.text =
                getString(R.string.text_action_no_tasks_today, it)
        }

        // respond to event
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
                                .setAction(getText(R.string.msg_action_undo)) {
                                    sharedViewModel.onUndoDeleteClick(event.thingToDo)
                                }.show()
                        }
                        is MainViewModel.SharedEvent.NavigateToTaskViewPager -> {
                            val action =
                                actionTodayTasksFragmentToTabPageTrackingStats(
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
                        else -> {
                            // do nothing
                        }
                    }.exhaustive
                }
            }
        }

        // add menu
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

                    R.id.action_hide_late_tasks -> {
                        menuItem.isChecked = !menuItem.isChecked
                        sharedViewModel.onHideLateClick(menuItem.isChecked)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
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
        binding.toolbar.subtitle = "Today's things to do"
        binding.textSup.text =
            SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(viewModel.startOfToday)
    }

    override fun onTaskChecked(thingToDo: Task, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
        if (isChecked) {
            soundPlayer.playSuccessSound()
        }
    }

    override fun onComposedTaskClick(thingToDo: ThingToDo) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: Task) {
        sharedViewModel.navigateToTaskInfoScreen(thingToDo)
    }

    override fun onProjectAddClick(composedTask: ThingToDo) {
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to composedTask.mainTask.id))
        sharedViewModel.addThingToDo()
    }

    override fun onSubTaskRightSwipe(thingToDo: Task) {
        sharedViewModel.deleteSubTask(thingToDo)
    }

    override fun onSubTaskLeftSwipe(thingToDo: Task) {
        sharedViewModel.updateSubTask(thingToDo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        menuProvider?.let { requireActivity().removeMenuProvider(it) }
    }
}
