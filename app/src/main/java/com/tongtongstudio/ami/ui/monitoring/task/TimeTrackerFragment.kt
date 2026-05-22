package com.tongtongstudio.ami.ui.monitoring.task

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.WorkSessionListener
import com.tongtongstudio.ami.adapter.WorkSessionsAdapter
import com.tongtongstudio.ami.data.datatables.WorkSession
import com.tongtongstudio.ami.databinding.FragmentTaskTimeTrackerBinding
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_CANCEL
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_PAUSE
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_START
import com.tongtongstudio.ami.services.TrackingService
import com.tongtongstudio.ami.timer.TimerType
import com.tongtongstudio.ami.timer.TrackingConstants
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.dialog.work_session.WORK_SESSION_LISTENER_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.work_session.WORK_SESSION_RESULT_KEY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimeTrackerFragment : Fragment(R.layout.fragment_task_time_tracker), WorkSessionListener {

    lateinit var binding: FragmentTaskTimeTrackerBinding
    lateinit var taskTitle: String
    private val viewModel: TaskDetailsAndTimeTrackerViewModel by viewModels({ requireParentFragment() })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTaskTimeTrackerBinding.bind(view)

        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.todayTasksFragment) {
                // Stop timer, remove notification and save time worked
                stopTrackingService()
            }
        }

        TrackingService.isRunning.observe(viewLifecycleOwner) {
            updateTracking(it)
        }

        TrackingService.timeInMillis.observe(viewLifecycleOwner) {
            viewModel.updateCurrentTime(it)
            if (viewModel.timerType == TimerType.COUNTDOWN) {
                val formattedTime = TrackingTimeUtility.getFormattedWorkingTime(
                    viewModel.curTimeInMillis,
                    TimerType.COUNTDOWN
                )
                binding.timerTextView.text = formattedTime
            }
        }

        TrackingService.timeWorkedInMillis.observe(viewLifecycleOwner) {
            if (viewModel.timerType == TimerType.STOPWATCH) {
                val formattedTime =
                    TrackingTimeUtility.getFormattedWorkingTime(
                        (viewModel.currentTotalWorkTime.value ?: 0) + it
                    )
                binding.timerTextView.text = formattedTime
            }
            updateProgressBar((viewModel.currentTotalWorkTime.value ?: 0) + it)
        }

        viewModel.currentTotalWorkTime.observe(viewLifecycleOwner) {
            binding.timerTextView.text =
                TrackingTimeUtility.getFormattedWorkingTime(it)
                    ?: getString(R.string.no_information)
            updateProgressBar(it ?: 0L)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    uiState.thingToDo?.let { task ->
                        taskTitle = task.taskRelations.mainTask.title
                        binding.apply {
                            // estimated work time view
                            val formattedTime = TrackingTimeUtility.getFormattedEstimatedTime(
                                task.taskRelations.mainTask.estimatedWorkingTime
                            )
                            tvTimeEstimated.text = if (formattedTime != null) {
                                getString(R.string.estimated_time_info, formattedTime)
                            } else {
                                getString(R.string.no_estimated_work_time)
                            }

                            // progress bar
                            if (task.taskRelations.mainTask.estimatedWorkingTime != null) {
                                circularProgressIndicator.max =
                                    (task.taskRelations.mainTask.estimatedWorkingTime / 1000).toInt()

                                // Observe current work time separately since it's not in uiState
                                viewModel.currentTotalWorkTime.observe(viewLifecycleOwner) { workTime ->
                                    circularProgressIndicator.progress =
                                        (workTime?.div(1000))?.toInt() ?: 0
                                    circularProgressIndicator.animate()
                                }
                            }
                        }
                    }
                }
            }
        }

        /*viewModel.task?.observe(viewLifecycleOwner) {
            taskTitle = it.taskRelations.mainTask.title
            binding.apply {
                // estimated work time view
                val formattedTime =
                    TrackingTimeUtility.getFormattedEstimatedTime(it.taskRelations.mainTask.estimatedWorkingTime)
                tvTimeEstimated.text = if (formattedTime != null) getString(
                    R.string.estimated_time_info,
                    formattedTime
                ) else getString(R.string.no_estimated_work_time)

                // progress bar
                if (it.taskRelations.mainTask.estimatedWorkingTime != null) {
                    circularProgressIndicator.max =
                        (it.taskRelations.mainTask.estimatedWorkingTime / 1000).toInt()
                    circularProgressIndicator.progress =
                        (viewModel.currentTotalWorkTime.value?.div(1000))?.toInt() ?: 0
                    circularProgressIndicator.animate()
                }
            }
        }*/
        val workSessionsAdapter = WorkSessionsAdapter(this)
        binding.apply {

            rvWorkSessions.apply {
                adapter = workSessionsAdapter
                layoutManager = LinearLayoutManager(this@TimeTrackerFragment.context)
                itemAnimator = DefaultItemAnimator()
            }
            // type of tracking time method
            trackingTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
                typeTextView.text = if (isChecked) {
                    sendCommandToService(TrackingConstants.ACTION_TIMER_TYPE_COUNTDOWN)
                    viewModel.updateTimerType(TimerType.COUNTDOWN)
                    getString(R.string.timer)
                } else {
                    sendCommandToService(TrackingConstants.ACTION_TIMER_TYPE_STOPWATCH)
                    viewModel.updateTimerType(TimerType.STOPWATCH)
                    getString(R.string.stopwatch)
                }
                // reset ui
                timerTextView.text = TrackingTimeUtility.getFormattedWorkingTime(
                    if (viewModel.timerType == TimerType.STOPWATCH)
                        viewModel.currentTotalWorkTime.value
                    else viewModel.curTimeInMillis, viewModel.timerType
                ) ?: getString(R.string.no_information)
            }

            fabPlay.setOnClickListener {
                startOrResumeTracking()
            }

            fabReset.setOnClickListener {
                onFabResetClicked()
            }

            addWorkTimeBtn.setOnClickListener {
                onBtnAddWorkTimeClicked()
            }

        }

        viewModel.workSessions.observe(viewLifecycleOwner) {
            workSessionsAdapter.submitList(it)
        }

        // from dialog add work time selection
        parentFragment?.setFragmentResultListener(WORK_SESSION_LISTENER_REQUEST_KEY) { _, bundle ->
            val workSession = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(WORK_SESSION_RESULT_KEY, WorkSession::class.java)
            } else {
                bundle.getParcelable(WORK_SESSION_RESULT_KEY)
            }
            if (workSession != null && workSession.parentTaskId != 0L) {
                viewModel.updateWorkSession(workSession)
                Snackbar.make(
                    view,
                    getString(R.string.msg_work_session_updated),
                    Snackbar.LENGTH_SHORT
                ).show()
            } else if (workSession != null) {
                viewModel.saveWorkSession(workSession)
                Snackbar.make(
                    view,
                    getString(R.string.msg_work_session_saved),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startOrResumeTracking() {
        viewModel.updateServiceState( true)
        if (viewModel.isTracking) {
            sendCommandToService(ACTION_PAUSE)
            binding.fabReset.isVisible = true
            // TODO: create a button and function aside --> no possibility to save from notif
            TrackingService.timeWorkedInMillis.value?.let { viewModel.saveTrackingTime(it) }
        } else sendCommandToService(ACTION_START)
    }

    private fun stopTrackingService() {
        if (viewModel.isServiceAlive) {
            if (viewModel.isTracking)
                TrackingService.timeWorkedInMillis.value?.let { viewModel.saveTrackingTime(it) }
            sendCommandToService(ACTION_CANCEL)
            viewModel.updateServiceState(false)
        }
    }

    private fun sendCommandToService(action: String) {
        Intent(requireContext(), TrackingService::class.java).also {
            it.putExtra("task_title", taskTitle)
            it.action = action
            // send command to service
            requireContext().startService(it)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        viewModel.updateTrackingState(isTracking)
        if (!isTracking) {
            binding.fabPlay.setImageResource(R.drawable.ic_play_arrow)
        } else {
            binding.fabPlay.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun updateProgressBar(millisUntilFinished: Long) {
        val progress = (millisUntilFinished / (1000 * 0.1 * 3600 )).toInt()
        binding.circularProgressIndicator.progress = progress
        //if (progress == binding.circularProgressIndicator.max)
    }

    private fun onBtnAddWorkTimeClicked() {
        val action = ViewPagerTrackingAndStatsFragmentDirections.actionViewPagerTrackingAndStatsFragmentToAddEditWorkSessionDialogFragment()
        if (findNavController().currentDestination?.id == R.id.viewPagerTrackingAndStatsFragment) {
            findNavController().navigate(action)
        }
    }

    private fun onFabResetClicked() {
        // reset chronometer
        sendCommandToService(ACTION_CANCEL)
        binding.fabReset.isVisible = false
        binding.timerTextView.text =
            TrackingTimeUtility.getFormattedWorkingTime(
                viewModel.curTimeInMillis,
                viewModel.timerType
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTrackingService()
    }

    override fun onClick(workSession: WorkSession) {
        val action = ViewPagerTrackingAndStatsFragmentDirections.actionViewPagerTrackingAndStatsFragmentToAddEditWorkSessionDialogFragment(
            workSession
        )
        if (findNavController().currentDestination?.id == R.id.viewPagerTrackingAndStatsFragment) {
            findNavController().navigate(action)
        }
    }

    override fun onRemoveClick(workSession: WorkSession) {
        viewModel.removeWorkSession(workSession)
    }
}