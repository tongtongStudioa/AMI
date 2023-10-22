package com.tongtongstudio.ami.ui.completed

import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import dagger.hilt.android.AndroidEntryPoint
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

@AndroidEntryPoint
class CompletedThingToDoFragment : Fragment(R.layout.fragment_main),
    ThingToDoListener {

    private val viewModel: CompletedThingToDoViewModel by viewModels()
    private lateinit var binding: FragmentMainBinding
    private lateinit var sharedViewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMainBinding.bind(view)

        // to make toolbar appear
        setUpToolbar()
        binding.fabAddTask.isVisible = false

        sharedViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        val completedAdapter = MainAdapter(this, requireContext())

        binding.apply {
            mainRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = completedAdapter
            }

            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val thingToDo = completedAdapter.data[viewHolder.adapterPosition]
                    if (direction == ItemTouchHelper.RIGHT) {
                        sharedViewModel.onThingToDoRightSwiped(thingToDo)
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

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            sharedViewModel.mainEvent.collect { event ->
                when (event) {
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
                    else -> {
                        // do nothing
                    }
                }
            }
        }

        viewModel.thingsToDoCompleted.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text = getString(R.string.text_explication_no_tasks_completed)
                binding.emptyRecyclerView.textViewActionText.text = getString(R.string.text_action_no_tasks_completed)
                binding.textSup.text = getString(R.string.nb_job_completed_info,0)
            } else {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
                completedAdapter.swapData(it)
                binding.textSup.text = getString(R.string.nb_job_completed_info,it.size)
            }
        }
    }

    override fun onItemThingToDoClicked(thingToDo: ThingToDo) {
        // TODO: 30/10/2022 view stat of the task
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
        //do nothing
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
        binding.toolbar.subtitle = "Things to do later"
    }

}