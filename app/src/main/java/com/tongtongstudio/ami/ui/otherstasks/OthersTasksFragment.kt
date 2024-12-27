package com.tongtongstudio.ami.ui.otherstasks

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
import androidx.fragment.app.setFragmentResultListener
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
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ThingToDoItemCallback
import com.tongtongstudio.ami.adapter.task.InteractionListener
import com.tongtongstudio.ami.adapter.task.ThingToDoAdapter
import com.tongtongstudio.ami.data.LaterFilter
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.ADD_DRAFT_TASK_OK
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OthersTasksFragment : Fragment(R.layout.fragment_main), InteractionListener {

    private val viewModel: OthersTasksViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var mainAdapter: ThingToDoAdapter
    private lateinit var textExplication: String

    override fun onCreate(savedInstanceState: Bundle?) {
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.middle_duration).toLong()
        }
        super.onCreate(savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        // set up the toolbar
        setUpToolbar()

        // empty text view explication
        textExplication = getString(R.string.text_explication_no_tasks_later)

        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        mainAdapter = ThingToDoAdapter(this)

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

            val callback = object : ThingToDoItemCallback(
                mainAdapter,
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
            ItemTouchHelper(callback).attachToRecyclerView(mainRecyclerView)
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
                    getString(R.string.text_action_no_tasks_later)
            } else {
                mainAdapter.submitList(it)
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
            }
        }

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
                            // Transition de sortie avec MaterialSharedAxis (DIRECTION X pour effet slide)
                            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            // Transition de retour avec MaterialSharedAxis
                            reenterTransition =
                                MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                                    duration =
                                        resources.getInteger(R.integer.middle_duration).toLong()
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
                                OthersTasksFragmentDirections.actionOthersTasksFragmentToLocalProjectStatsFragment2(
                                    event.composedTaskData
                                )
                            findNavController().navigate(action)
                        }

                        else -> {}
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
            exitTransition = MaterialFadeThrough().apply {
                duration = resources.getInteger(R.integer.middle_duration).toLong()
            }
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
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to composedTask.mainTask.id))
        sharedViewModel.addThingToDo()
    }

    override fun onSubTaskRightSwipe(thingToDo: ThingToDo) {
        //TODO("Not yet implemented")
    }

    override fun onSubTaskLeftSwipe(thingToDo: ThingToDo) {
        //TODO("Not yet implemented")
    }
}