package com.tongtongstudio.ami.ui.monitoring.project

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.task.InteractionListener
import com.tongtongstudio.ami.adapter.task.SubTaskAdapter
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.FragmentProjectDetailsBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProjectDetailsFragment : Fragment(R.layout.fragment_project_details), InteractionListener {
    lateinit var binding: FragmentProjectDetailsBinding
    private val viewModel: ProjectDetailsViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProjectDetailsBinding.bind(view)
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setUpToolbar()
        updateProgressBar()

        // TODO: Change this method to update sub tasks list if a task is removed
        val subTasksAdapter = SubTaskAdapter(this, viewModel.subTasks)
        binding.apply {
            tvProjectTitle.text = viewModel.projectName
            tvDescription.text = viewModel.description ?: ""

            estimatedTime.text =
                TrackingTimeUtility.getFormattedTimeWorked(viewModel.estimatedTime)
                    ?: getText(R.string.no_information)
            totalWorkTime.text =
                TrackingTimeUtility.getFormattedTimeWorked(viewModel.workTime)
                    ?: getText(R.string.no_information)
            rvSubtasks.apply {
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
                adapter = subTasksAdapter
            }
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
                    subTasksAdapter.subTasks[viewHolder.bindingAdapterPosition]
                if (direction == ItemTouchHelper.RIGHT) {
                    onSubTaskRightSwipe(subTask)
                } else if (direction == ItemTouchHelper.LEFT) {
                    onSubTaskLeftSwipe(subTask)
                }
            }

        }).attachToRecyclerView(binding.rvSubtasks)
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

    private fun updateProgressBar() {
        val progress =
            viewModel.subTasks.sumOf { if (it.isCompleted && !it.isDraft) it.priority!! else 0 }
        val totalPriority = viewModel.subTasks.sumOf { it.priority ?: 0 }
        val progressPercentage =
            progress / (if (viewModel.subTasks.isEmpty()) 1F else totalPriority
                .toFloat()) * 100

        // Update text progress
        binding.progressText.text = getString(R.string.completion_rate_value, progressPercentage)
        binding.projectProgress.progress = progress.toInt()

    }

    override fun onTaskChecked(thingToDo: Task, isChecked: Boolean, position: Int) {
        sharedViewModel.onCheckBoxChanged(thingToDo, isChecked)
        if (isChecked) {
            //soundPlayer.playSuccessSound()
        }
    }

    override fun onComposedTaskClick(thingToDo: ThingToDo) {
        sharedViewModel.navigateToTaskComposedInfoScreen(thingToDo)
    }

    override fun onTaskClick(thingToDo: Task, itemView: View) {
        sharedViewModel.navigateToTaskInfoScreen(thingToDo, itemView)
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

}