package com.tongtongstudio.ami.ui.todaytasks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ThingToDoItemCallback
import com.tongtongstudio.ami.adapter.thingToDo.InteractionListener
import com.tongtongstudio.ami.adapter.thingToDo.ThingToDoAdapter
import com.tongtongstudio.ami.data.SortOrder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.notification.SoundPlayer
import com.tongtongstudio.ami.ui.ADD_DRAFT_TASK_OK
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.ui.todaytasks.TodayTasksFragmentDirections.Companion.actionTodayTasksFragmentToAddEditTaskFragment
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
    private val sharedViewModel: MainViewModel by activityViewModels()
    private lateinit var soundPlayer: SoundPlayer
    private var menuProvider: MenuProvider? = null

    //private lateinit var sharedPreferences: SharedPreferences
    //private var tutorialTrigger: TutorialTrigger? = null

    private lateinit var recyclerView: RecyclerView
    private val axisForward by lazy {
        MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }
    }

    private val axisBackward by lazy {
        MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }
    }

    /*override fun onAttach(context: Context) {
        super.onAttach(context)
        tutorialTrigger = context as? TutorialTrigger
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        //sharedPreferences = requireActivity().getSharedPreferences(PREF_TUTORIAL, Context.MODE_PRIVATE)
        exitTransition = axisBackward
        reenterTransition = axisForward

        /**
         * The below code is required to animate correctly when the user returns from [DetailFragment].
         */
        postponeEnterTransition()
        //Sound player and adapter
        mainTaskAdapter = ThingToDoAdapter(this, false)
        soundPlayer = SoundPlayer(requireContext())

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        recyclerView = binding.mainRecyclerView

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
                (requireView().parent as ViewGroup).viewTreeObserver
                    .addOnPreDrawListener {
                        startPostponedEnterTransition()
                        true
                    }
            }
        }

        viewModel.upcomingTasksCount.observe(viewLifecycleOwner) {
            binding.emptyRecyclerView.textViewActionText.text =
                getString(R.string.text_action_no_tasks_today, it)
        }

        // respond to event
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                                .setAction(getText(R.string.msg_action_undo)) {
                                    sharedViewModel.onUndoDeleteClick(event.thingToDo)
                                }.show()
                        }
                        is MainViewModel.SharedEvent.NavigateToTaskViewPager -> {
                            exitTransition = MaterialElevationScale(false).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition = MaterialElevationScale(true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            val action =
                                TodayTasksFragmentDirections.actionTodayTasksFragmentToTabPageTrackingStats(
                                    event.task.id,

                                )
                            val extras =
                                FragmentNavigatorExtras(event.sharedView to event.sharedView.transitionName)
                            findNavController().navigate(action, extras)
                        }
                        is MainViewModel.SharedEvent.NavigateToProjectDetailsScreen -> {
                            exitTransition = MaterialElevationScale(false).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition = MaterialElevationScale(true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            val action =
                                TodayTasksFragmentDirections.actionTodayTasksFragmentToLocalProjectStatsFragment2(
                                    event.project.taskRelations.mainTask.id
                                )
                            val extras =
                                FragmentNavigatorExtras(event.sharedView to event.sharedView.transitionName)
                            findNavController().navigate(action,extras)
                        }
                        is MainViewModel.SharedEvent.ShowMissedRecurringTaskDialog -> {
                            val action =
                                TodayTasksFragmentDirections.actionTodayTasksFragmentToMissedRecurringTasksDialogFragment(
                                    event.missedTasks.toTypedArray()
                                )
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToAddScreenWithParentTask -> {
                            val action = actionTodayTasksFragmentToAddEditTaskFragment(
                                title = getString(R.string.fragment_title_add_thing_to_do),
                                thingToDo = null,
                                parentTask = event.parentTask
                            )
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToDraftScreen -> {
                            val action =
                                TodayTasksFragmentDirections.actionTodayTasksFragmentToDraftsFragment()
                            findNavController().navigate(action)
                        }
                        is MainViewModel.SharedEvent.ShowAssessmentCompleted -> {
                            Snackbar.make(
                                binding.fabAddTask,
                                if (event.result) getString(R.string.assessment_completed) else getString(R.string.assessment_delayed),
                                Toast.LENGTH_SHORT
                            ).setAnchorView(binding.fabAddTask)
                                .show()
                        }
                        else -> {
                            // do nothing
                        }
                    }
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
                    menu.findItem(R.id.action_hide_late_tasks).isChecked =
                        viewModel.preferencesFlow.first().hideLate
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
        }, viewLifecycleOwner, Lifecycle.State.STARTED)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //collapse toolbar
        setUpToolbar()

        // implement UI
        binding.apply {
            fabAddTask.setOnClickListener {
                sharedViewModel.addThingToDo()
            }

            mainRecyclerView.apply {
                adapter = mainTaskAdapter
                layoutManager = LinearLayoutManager(requireContext())
                //setHasFixedSize(false)
                itemAnimator = null
            }

            val callback = object : ThingToDoItemCallback<ThingToDoAdapter>(
                mainTaskAdapter,
                ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
                requireContext()
            ) {
                override fun actionOnRightSwiped(thingToDo: ThingToDo, position: Int) {
                    // delete task
                    sharedViewModel.deleteTask(thingToDo, requireContext())
                    //mainTaskAdapter.notifyItemRemoved(position)
                }

                override fun actionLeftSwiped(thingToDo: ThingToDo, position: Int) {
                    //update task
                    sharedViewModel.updateTask(thingToDo)
                    //mainTaskAdapter.notifyItemChanged(position)
                }
            }
            ItemTouchHelper(callback).attachToRecyclerView(mainRecyclerView)
        }

        //tutorialTrigger?.triggerTutorialFor(this)
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
        binding.toolbar.subtitle = "Today's things to do"
        binding.textSup.text =
            SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(viewModel.startOfToday)
    }

    override fun onTaskChecked(thingToDo: ThingToDo, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
        if (isChecked) {
            soundPlayer.playSuccessSound()
        }
    }

    override fun onProjectClick(thingToDo: ThingToDo, itemView: View, position: Int) {
        sharedViewModel.navigateToProjectDetailsScreen(thingToDo,itemView, position)
    }

    override fun onTaskClick(thingToDo: Task, itemView: View, position: Int) {
        sharedViewModel.navigateToTaskDetailsAndTrackScreen(thingToDo, itemView, position)
    }

    override fun onProjectAddClick(thingToDo: ThingToDo) {
        sharedViewModel.addSubThingTodo(thingToDo.taskRelations.mainTask)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        menuProvider?.let { requireActivity().removeMenuProvider(it) }
    }
}
