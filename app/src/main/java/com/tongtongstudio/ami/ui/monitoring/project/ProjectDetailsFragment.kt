package com.tongtongstudio.ami.ui.monitoring.project

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.task.InteractionListener
import com.tongtongstudio.ami.adapter.task.SubTaskAdapter
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentProjectDetailsBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.ADD_DRAFT_TASK_OK
import com.tongtongstudio.ami.ui.ADD_TASK_RESULT_OK
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import com.tongtongstudio.ami.util.exhaustive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProjectDetailsFragment : Fragment(R.layout.fragment_project_details), InteractionListener {
    lateinit var binding: FragmentProjectDetailsBinding
    private val viewModel: ProjectDetailsViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var subTaskAdapter: SubTaskAdapter

    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectDetailsBinding.bind(view)
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setUpToolbar()


        subTaskAdapter = SubTaskAdapter(this)

        binding.apply {
            tvProjectTitle.text = viewModel.projectName
            tvDescription.text = viewModel.description ?: ""

            fabAddSubTask.setOnClickListener {
                sharedViewModel.addThingToDo()
            }
            estimatedTime.text =
                TrackingTimeUtility.getFormattedTimeWorked(viewModel.estimatedTime)
                    ?: getText(R.string.no_information)
            totalWorkTime.text =
                TrackingTimeUtility.getFormattedTimeWorked(viewModel.workTime)
                    ?: getText(R.string.no_information)
            rvSubtasks.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                adapter = subTaskAdapter
            }
        }

        viewModel.subTasks.observe(viewLifecycleOwner) {
            subTaskAdapter.swapData(it)
            updateProgressBar(it)
        }
        updateEstimatedTimeIndicator()

        // TODO: resolve sub item touch behavior
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val subTask: Task =
                    subTaskAdapter.subTasks[viewHolder.bindingAdapterPosition]
                if (direction == ItemTouchHelper.RIGHT) {
                    onSubTaskRightSwipe(subTask)
                } else if (direction == ItemTouchHelper.LEFT) {
                    onSubTaskLeftSwipe(subTask)
                }
            }

        }).attachToRecyclerView(binding.rvSubtasks)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.mainEvent.collect { event ->
                    when (event) {
                        is MainViewModel.SharedEvent.NavigateToEditScreen -> {
                            val action =
                                ProjectDetailsFragmentDirections.actionProjectDetailsFragmentToAddEditTaskFragment(
                                    getString(R.string.fragment_title_edit_thing_to_do),
                                    event.task

                                )
                            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition =
                                MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                                    duration =
                                        resources.getInteger(R.integer.middle_duration).toLong()
                                }
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToAddScreen -> {
                            val action =
                                ProjectDetailsFragmentDirections.actionProjectDetailsFragmentToAddEditTaskFragment(
                                    getString(R.string.fragment_title_add_thing_to_do),
                                    null
                                )
                            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
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

                        is MainViewModel.SharedEvent.NavigateToTaskViewPager -> {
                            val action =
                                ProjectDetailsFragmentDirections.actionProjectDetailsFragmentToViewPagerTrackingAndStatsFragment(
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

                        else -> {
                            // do nothing
                        }
                    }.exhaustive
                }
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
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.toolbar.setNavigationOnClickListener {
            navController.navigateUp(appBarConfiguration)
        }
    }

    private fun updateEstimatedTimeIndicator() {
        if ((viewModel.estimatedTime ?: 0) < viewModel.workTime)
            binding.estimatedTime.setTextColor(resources.getColor(R.color.design_default_color_error))
    }

    private fun updateProgressBar(subTasks: List<Task>) {
        val progress =
            subTasks.sumOf { if (it.isCompleted && it.priority != null) it.priority else 0 }
        val totalPriority = subTasks.sumOf { it.priority ?: 0 }
        val progressPercentage =
            progress / (if (subTasks.isEmpty() || totalPriority == 0) 1F else totalPriority
                .toFloat()) * 100

        // Update text progress
        binding.progressText.text = getString(R.string.completion_rate_value, progressPercentage)
        binding.projectProgress.progress = progressPercentage.toInt()

    }

    override fun onTaskChecked(thingToDo: Task, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
        if (isChecked) {
            //soundPlayer.playSuccessSound()
        }
    }

    override fun onProjectClick(thingToDo: ThingToDo) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: Task, itemView: View) {
        sharedViewModel.navigateToTaskDetailsAndTrackScreen(thingToDo, itemView)
    }

    override fun onProjectAddClick(thingToDo: ThingToDo) {
        setFragmentResult("is_new_sub_task", bundleOf("project_id" to thingToDo.mainTask.id))
        sharedViewModel.addThingToDo()
    }

    override fun onSubTaskRightSwipe(thingToDo: Task) {
        sharedViewModel.deleteSubTask(thingToDo)
    }

    override fun onSubTaskLeftSwipe(thingToDo: Task) {
        sharedViewModel.updateSubTask(thingToDo)
    }

}