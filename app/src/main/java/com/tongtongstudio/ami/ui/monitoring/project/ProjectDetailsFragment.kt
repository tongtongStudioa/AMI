package com.tongtongstudio.ami.ui.monitoring.project

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ThingToDoItemCallback
import com.tongtongstudio.ami.adapter.thingToDo.InteractionListener
import com.tongtongstudio.ami.adapter.thingToDo.ThingToDoAdapter
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
    private lateinit var subTaskAdapter: ThingToDoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = resources.getInteger(R.integer.long_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectDetailsBinding.bind(view)
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        // Shared transition id
        ViewCompat.setTransitionName(binding.projectDetails, "shared_element_${viewModel.projectId}")

        setUpToolbar()

        subTaskAdapter = ThingToDoAdapter(this)

        binding.apply {
            fabAddSubTask.setOnClickListener {
                viewModel.project?.value?.taskRelations?.mainTask?.let { mainTask ->
                    sharedViewModel.addSubThingTodo(mainTask)
                }
            }
            totalWorkTime.text =
                TrackingTimeUtility.getFormattedTimeWorked(viewModel.getProjectWorkTime())
                    ?: getText(R.string.no_information)
            rvSubtasks.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                adapter = subTaskAdapter
            }
        }

        viewModel.project?.observe(viewLifecycleOwner) {
            updateProjectInformation(it)
        }
        viewModel.subTasks.observe(viewLifecycleOwner) {
            subTaskAdapter.submitList(it)
        }

        val callback = object : ThingToDoItemCallback<ThingToDoAdapter>(
            subTaskAdapter,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
            requireContext()
        ) {
            override fun actionOnRightSwiped(thingToDo: ThingToDo, position: Int) {
                // delete task
                sharedViewModel.deleteTask(thingToDo, requireContext())
                subTaskAdapter.notifyItemRemoved(position)
            }

            override fun actionLeftSwiped(thingToDo: ThingToDo, position: Int) {
                //update task
                sharedViewModel.updateTask(thingToDo)
                subTaskAdapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvSubtasks)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedViewModel.mainEvent.collect { event ->
                    when (event) {
                        is MainViewModel.SharedEvent.NavigateToEditScreen -> {
                            val action =
                                ProjectDetailsFragmentDirections.actionProjectDetailsFragmentToAddEditTaskFragment(
                                    getString(R.string.fragment_title_edit_thing_to_do),
                                     event.thingToDo
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
                                    getString(R.string.fragment_title_add_thing_to_do)
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

                        is MainViewModel.SharedEvent.NavigateToAddScreenWithParentTask -> {
                            val action = ProjectDetailsFragmentDirections.actionProjectDetailsFragmentToAddEditTaskFragment(
                                title = getString(R.string.fragment_title_add_thing_to_do),
                                thingToDo = null,
                                parentTask = event.parentTask
                            )
                            exitTransition = MaterialElevationScale(false).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            reenterTransition = MaterialElevationScale(true).apply {
                                duration = resources.getInteger(R.integer.middle_duration).toLong()
                            }
                            findNavController().navigate(action)
                        }

                        is MainViewModel.SharedEvent.NavigateToProjectDetailsScreen -> {
                            val action =
                                ProjectDetailsFragmentDirections.actionProjectDetailsFragmentSelf(
                                    event.project.taskRelations.mainTask.id
                                )
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
                                    event.task.id
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

    private fun updateProjectInformation(project: ThingToDo) {
        binding.apply {
            tvProjectTitle.text = project.taskRelations.mainTask.title
            tvDescription.text = project.taskRelations.mainTask.description ?: ""
            estimatedTime.text =
                TrackingTimeUtility.getFormattedTimeWorked(project.taskRelations.mainTask.estimatedWorkingTime)
                    ?: getText(R.string.no_information)
            val totalEstimatedWorkTime = viewModel.getEstimatedWorkTime()
            totalEstimatedTime.text = if (totalEstimatedWorkTime != null) "(" + TrackingTimeUtility.getFormattedTimeWorked(viewModel.getEstimatedWorkTime()) + ")"
            else ""
            totalEstimatedTime.isVisible = totalEstimatedWorkTime != null

            progressText.text = getString(R.string.completion_rate_value, project.completionRate ?: 0F)
            projectProgress.progress = project.completionRate?.toInt() ?: 0
            val projectEstimatedTime = project.taskRelations.mainTask.estimatedWorkingTime
            if (projectEstimatedTime != null && projectEstimatedTime < viewModel.getProjectWorkTime())
                estimatedTime.setTextColor(resources.getColor(R.color.design_default_color_error))
        }
    }

    override fun onTaskChecked(thingToDo: ThingToDo, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
        if (isChecked) {
            //soundPlayer.playSuccessSound()
        }
    }

    override fun onProjectClick(thingToDo: ThingToDo, itemView: View) {
        sharedViewModel.navigateToProjectDetailsScreen(thingToDo,itemView)
    }

    override fun onTaskClick(thingToDo: Task, itemView: View) {
        sharedViewModel.navigateToTaskDetailsAndTrackScreen(thingToDo, itemView)
    }

    override fun onProjectAddClick(thingToDo: ThingToDo) {
        sharedViewModel.addSubThingTodo(thingToDo.taskRelations.mainTask)
    }

}