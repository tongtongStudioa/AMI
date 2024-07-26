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
import com.tongtongstudio.ami.adapter.CustomItemTouchHelperCallback
import com.tongtongstudio.ami.adapter.task.InteractionListener
import com.tongtongstudio.ami.adapter.task.TaskAdapter
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.FragmentMainBinding
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

@AndroidEntryPoint
class CompletedThingToDoFragment : Fragment(R.layout.fragment_main),
    InteractionListener {

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
        val completedAdapter = TaskAdapter(this)

        binding.apply {
            mainRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = completedAdapter
            }

            val callback = CustomItemTouchHelperCallback(
                completedAdapter,
                { newSubTask, parentId ->
                    // TODO: use drag and drop to add subTTask
                    //sharedViewModel.addSubTask(newSubTask,parentId)
                },
                { taskWithSubTasks ->
                    //delete task
                    //newTaskAdapter.notifyItemRemoved(viewHolder.absoluteAdapterPosition)
                    sharedViewModel.deleteTask(taskWithSubTasks)
                },
                {
                    // do nothing
                }, requireContext()
            )
            ItemTouchHelper(callback).attachToRecyclerView(mainRecyclerView)

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
                    val thingToDo = completedAdapter.getTaskList()[viewHolder.adapterPosition]
                    if (direction == ItemTouchHelper.RIGHT) {
                        sharedViewModel.deleteTask(thingToDo)
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
                    is MainViewModel.SharedEvent.NavigateToTaskDetailsScreen -> {
                        val action =
                            CompletedThingToDoFragmentDirections.actionCompletedThingToDoFragmentToDetailsFragment(
                                event.task
                            )
                        findNavController().navigate(action)
                    }
                    is MainViewModel.SharedEvent.NavigateToLocalProjectStatsScreen -> {
                        val action =
                            CompletedThingToDoFragmentDirections.actionCompletedThingToDoFragmentToLocalProjectStatsFragment2(
                                event.composedTaskData
                            )
                        findNavController().navigate(action)
                    }
                    else -> {
                        //do nothing
                    }
                }
            }
        }

        viewModel.thingsToDoCompleted.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = true
                binding.mainRecyclerView.isVisible = false
                binding.emptyRecyclerView.textViewExplication.text =
                    getString(R.string.text_explication_no_tasks_completed)
                binding.emptyRecyclerView.textViewActionText.text =
                    getString(R.string.text_action_no_tasks_completed)
                binding.textSup.text = getString(R.string.nb_job_completed_info, 0)
            } else {
                binding.emptyRecyclerView.viewEmptyRecyclerView.isVisible = false
                binding.mainRecyclerView.isVisible = true
                completedAdapter.submitList(it)
                binding.textSup.text = getString(R.string.nb_job_completed_info, it.size)
            }
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
        binding.toolbar.subtitle = getString(R.string.completed_tasks_subtitle)
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
        // do nothing
    }

    override fun onSubTaskRightSwipe(thingToDo: Ttd) {
        sharedViewModel.deleteSubTask(thingToDo)
    }

    override fun onSubTaskLeftSwipe(thingToDo: Ttd) {
        sharedViewModel.updateSubTask(thingToDo)
    }

}