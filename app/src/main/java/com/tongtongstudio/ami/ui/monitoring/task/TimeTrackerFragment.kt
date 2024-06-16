package com.tongtongstudio.ami.ui.monitoring.task

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.FragmentTaskTimeTrackerBinding
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_CANCEL
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_PAUSE
import com.tongtongstudio.ami.notification.TimerNotification.Companion.ACTION_START
import com.tongtongstudio.ami.services.TrackingService
import com.tongtongstudio.ami.timer.TimerType
import com.tongtongstudio.ami.timer.TrackingConstants
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_DIALOG_TAG
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_LISTENER_REQUEST_KEY
import com.tongtongstudio.ami.ui.dialog.ESTIMATED_TIME_RESULT_KEY
import com.tongtongstudio.ami.ui.dialog.EstimatedTimeDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimeTrackerFragment : Fragment(R.layout.fragment_task_time_tracker) {

    lateinit var binding: FragmentTaskTimeTrackerBinding
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
            viewModel.curTimeInMillis = it
            if (viewModel.timerType == TimerType.COUNTDOWN) {
                val formattedTime = TrackingTimeUtility.getFormattedWorkingTime(
                    viewModel.curTimeInMillis,
                    TimerType.COUNTDOWN
                )
                binding.timerTextView.text = formattedTime
            }
        }

        TrackingService.timeWorkedInMillis.observe(viewLifecycleOwner) {
            viewModel.timeWorked = it
            if (viewModel.timerType == TimerType.STOPWATCH) {
                val formattedTime =
                    TrackingTimeUtility.getFormattedWorkingTime(
                        (viewModel.actualWorkTime.value ?: 0) + it
                    )
                binding.timerTextView.text = formattedTime
            }
            updateProgressBar((viewModel.actualWorkTime.value ?: 0) + it)
        }

        binding.apply {
            timerTextView.text = TrackingTimeUtility.getFormattedWorkingTime(
                viewModel.primaryWorkTime ?: 0
            ) ?: getString(R.string.no_information)

            // estimated work time view
            val formattedTime =
                TrackingTimeUtility.getFormattedEstimatedTime(viewModel.estimatedWorkingTime)
            tvTimeEstimated.text = if (formattedTime != null) getString(
                R.string.estimated_time_info,
                formattedTime
            ) else getString(R.string.no_estimated_work_time)

            // type of tracking time method
            trackingTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
                typeTextView.text = if (isChecked) {
                    sendCommandToService(TrackingConstants.ACTION_TIMER_TYPE_COUNTDOWN)
                    viewModel.timerType = TimerType.COUNTDOWN
                    getString(R.string.timer)
                } else {
                    sendCommandToService(TrackingConstants.ACTION_TIMER_TYPE_STOPWATCH)
                    viewModel.timerType = TimerType.STOPWATCH
                    getString(R.string.stopwatch)
                }
                // reset ui
                timerTextView.text = TrackingTimeUtility.getFormattedWorkingTime(
                    if (viewModel.timerType == TimerType.STOPWATCH)
                        viewModel.actualWorkTime.value
                    else viewModel.curTimeInMillis, viewModel.timerType
                ) ?: getString(R.string.no_information)
            }
            // progress bar
            if (viewModel.estimatedWorkingTime != null) {
                circularProgressIndicator.max = (viewModel.estimatedWorkingTime!! / 1000).toInt()
                circularProgressIndicator.progress =
                    (viewModel.actualWorkTime.value?.div(1000))?.toInt() ?: 0
                circularProgressIndicator.animate()
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

            // from dialog add work time selection
            setFragmentResultListener(ESTIMATED_TIME_LISTENER_REQUEST_KEY) { _, bundle ->
                val resultValues = bundle.getIntArray(ESTIMATED_TIME_RESULT_KEY)
                val hours: Int? = resultValues?.get(0)
                val minutes: Int? = resultValues?.get(1)
                if (hours != null && minutes != null) {
                    // TODO: calculus in viewModel
                    val addedWorkTimeInMillis: Long =
                        (hours.toLong() * 60 * 60 * 1000) + (minutes.toLong() * 60 * 1000)
                    viewModel.saveTrackingTime(addedWorkTimeInMillis)
                    Snackbar.make(
                        view,
                        getString(R.string.msg_work_session_saved),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    if (viewModel.timerType == TimerType.STOPWATCH)
                        Toast.makeText(context, "stopwatch", Toast.LENGTH_SHORT).show()
                    binding.timerTextView.text = TrackingTimeUtility.getFormattedWorkingTime(
                        viewModel.actualWorkTime.value ?: 0,
                        TimerType.STOPWATCH
                    )
                }
            }
        }
    }

    private fun startOrResumeTracking() {
        viewModel.isServiceAlive = true
        if (viewModel.isTracking) {
            sendCommandToService(ACTION_PAUSE)
            binding.fabReset.isVisible = true
            // TODO: create a button and function aside --> no possibility to save from notif
            viewModel.saveTrackingTime()
        } else sendCommandToService(ACTION_START)
    }

    private fun stopTrackingService() {
        if (viewModel.isServiceAlive) {
            if (viewModel.isTracking)
                viewModel.saveTrackingTime()
            sendCommandToService(ACTION_CANCEL)
            viewModel.isServiceAlive = false
        }
    }

    private fun sendCommandToService(action: String) {
        Intent(requireContext(), TrackingService::class.java).also {
            it.putExtra("task_title", viewModel.name)
            it.action = action
            // send command to service
            requireContext().startService(it)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        viewModel.isTracking = isTracking
        if (!isTracking) {
            binding.fabPlay.setImageResource(R.drawable.ic_play_arrow)
        } else {
            binding.fabPlay.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun updateProgressBar(millisUntilFinished: Long) {
        if (viewModel.estimatedWorkingTime != null) {
            val progress = (millisUntilFinished / 1000).toInt()
            binding.circularProgressIndicator.progress = progress
            //if (progress == binding.circularProgressIndicator.max)
        }
    }

    private fun onBtnAddWorkTimeClicked() {
        val newFragment = EstimatedTimeDialogFragment("Add work time")
        newFragment.show(parentFragmentManager, ESTIMATED_TIME_DIALOG_TAG)
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
}