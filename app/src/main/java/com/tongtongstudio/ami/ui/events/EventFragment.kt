package com.tongtongstudio.ami.ui.events

import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.tongtongstudio.ami.data.datatables.ProjectWithSubTasks
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.todolistami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

@AndroidEntryPoint
class EventFragment : Fragment(R.layout.fragment_main), ThingToDoListener {
    private val viewModel: EventViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var mainAdapter: MainAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMainBinding.bind(view)

        setUpToolbar()

        sharedViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

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
                        loadEvents()
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

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            sharedViewModel.onAddEditResult(
                result,
                resources.getStringArray(R.array.thing_to_do_added),
                resources.getStringArray(R.array.thing_to_do_updated)
            )
        }

        loadEvents()

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            sharedViewModel.mainEvent.collect { event ->
                when (event) {
                    is MainViewModel.SharedEvent.NavigateToEditScreen -> {
                        val action =
                            EventFragmentDirections.actionEventFragmentToAddEditTaskFragment(
                                getString(R.string.fragment_title_edit_thing_to_do),
                                event.thingToDo
                                // TODO: 06/09/2022 change to resource string

                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.NavigateToAddScreen -> {
                        val action =
                            EventFragmentDirections.actionEventFragmentToAddEditTaskFragment(
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
                            getString(R.string.msg_thing_to_do_deleted),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getString(R.string.msg_action_undo)) {
                                sharedViewModel.onUndoDeleteClick(event.thingToDo)
                            }.show()
                    }
                    is MainViewModel.SharedEvent.NavigateToTrackingScreen -> {
                        // do nothing
                    }
                    is MainViewModel.SharedEvent.NavigateToLocalProjectStatsScreen -> {
                        // do nothing
                    }
                    is MainViewModel.SharedEvent.ShowMissedRecurringTaskDialog -> {
                        // do nothing
                    }
                }.exhaustive
            }
        }
    }

    private fun loadEvents() {
        viewModel.events.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text =
                    getString(R.string.text_explication_no_events)
                binding.emptyRecyclerView.textViewActionText.text =
                    getString(R.string.text_action_no_events)
            } else {
                mainAdapter.swapData(it)
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
            }
        }
    }

    override fun onItemThingToDoClicked(thingToDo: ThingToDo) {
        //TODO("Not yet implemented")
    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean, position: Int) {
        //TODO("Not yet implemented")
    }

    override fun onItemTaskSwiped(subTask: Task, dir: Int) {
        //TODO("Not yet implemented")
    }

    override fun onProjectBtnAddSubTaskClicked(projectData: ProjectWithSubTasks) {
        //TODO("Not yet implemented")
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