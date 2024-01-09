package com.tongtongstudio.ami.ui.monitoring.trackingtime

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.TaskTrackingFragmentBinding
import com.tongtongstudio.ami.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingTimeAndDetailsFragment : Fragment(R.layout.task_tracking_fragment) {

    lateinit var binding: TaskTrackingFragmentBinding
    private val viewModel: TrackingTimeAndDetailsViewModel by viewModels()
    private lateinit var chronometer: Chronometer
    private lateinit var soundPool: SoundPool
    private var soundID: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = TaskTrackingFragmentBinding.bind(view)

        setUpToolbar()
        chronometer = binding.chronometerWidget

        chronometer.base = SystemClock.elapsedRealtime() - viewModel.timerCount

        // sound pool for chronometer
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            SoundPool(6, AudioManager.STREAM_MUSIC, 0)
        }
        soundID = soundPool.load(this.context, R.raw.success_1, 1)

        // binding elements layout
        binding.apply {
            // task info
            if (viewModel.task != null) {
                taskName.text = viewModel.task!!.title
                taskDescription.text = viewModel.task!!.description
                if (viewModel.task!!.startDate != null) {
                    taskStartDate.text =
                        viewModel.task!!.getDateFormatted(viewModel.task!!.startDate) ?: ""
                    taskStartDate.isVisible = true
                } else taskStartDate.isVisible = false
                taskDeadline.text =
                    viewModel.task!!.getDateFormatted(viewModel.task!!.dueDate) ?: ""
                /*if (viewModel.task!!.taskEvaluationDescription != null) {
                    taskEvaluationDescription.text =
                        viewModel.task!!.taskEvaluationDescription ?: ""
                    tvEvaluationDate.text =
                        if (viewModel.task!!.taskEvaluationDate != null) DateFormat.getDateInstance(
                            DateFormat.SHORT
                        ).format(viewModel.task!!.taskEvaluationDate!!) else ""

                } else taskEvaluation.isVisible = false*/
                taskEvaluation.isVisible = false
            }
            // estimated work time view
            val estimatedWorkTime = viewModel.retrieveEstimatedTime()
            tvTimeEstimated.text = if (estimatedWorkTime != null) {
                val hours = (estimatedWorkTime / 3600_000).toInt()
                val minutes = (estimatedWorkTime / 60_000 % 60).toInt()
                if (hours > 1) {
                    val minutesToString = if (minutes < 10) {
                        "0$minutes"
                    } else minutes.toString()
                    getString(R.string.estimated_time_info, hours, minutesToString)
                } else minutes.toString() + "min"
            } else getString(R.string.no_estimated_work_time)


            // stats view
            if (viewModel.streak == null && viewModel.totalRepetition == null)
                statsView.isVisible = false

            tvNbCompleted.text = if (viewModel.totalRepetition == null)
                viewModel.totalRepetition.toString()
            else getString(R.string.no_information)
            binding.tvStreak.text =
                if (viewModel.streak != null)
                    viewModel.streak.toString()
                else getString(R.string.no_information)


            fabPlay.setOnClickListener {
                if (viewModel.isTimerRunning) {
                    onPauseClicked()
                } else onPlayClicked()
            }
            fabStop.setOnClickListener {
                onFabStopClicked()
            }

            var haveBeenPlayed = false
            if (estimatedWorkTime != null && estimatedWorkTime != 0L && viewModel.timerCount < estimatedWorkTime * 60 * 1000) {
                val beginningBase = chronometer.base
                chronometer.setOnChronometerTickListener {
                    if (viewModel.timerCount + it.base - beginningBase > estimatedWorkTime * 60 * 1000 && !haveBeenPlayed)
                        haveBeenPlayed = playAlertSound()
                }
            }
        }
    }

    private fun onFabStopClicked() {
        chronometer.stop()
        chronometer.base = SystemClock.elapsedRealtime()
        viewModel.timerCount = 0
        viewModel.isTimerRunning = false

        binding.fabPlay.setImageResource(R.drawable.ic_play_arrow)
    }

    private fun onPlayClicked() {
        chronometer.base = SystemClock.elapsedRealtime() - viewModel.timerCount
        chronometer.start()
        viewModel.isTimerRunning = true

        binding.fabStop.isVisible = false
        binding.fabPlay.setImageResource(R.drawable.ic_pause)
    }

    private fun onPauseClicked() {
        chronometer.stop()
        viewModel.saveTrackingTime(SystemClock.elapsedRealtime() - chronometer.base)
        viewModel.isTimerRunning = false

        binding.fabStop.isVisible = true
        binding.fabPlay.setImageResource(R.drawable.ic_play_arrow)
        Snackbar.make(requireView(), "Work time saved", Snackbar.LENGTH_SHORT).show()
    }

    override fun onStop() {
        if (viewModel.isTimerRunning) {
            viewModel.saveTrackingTime(SystemClock.elapsedRealtime() - chronometer.base)
        }

        super.onStop()
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

    private fun playAlertSound(): Boolean {
        if (soundID != 0)
            soundPool.play(soundID, 0.1F, 0.1F, 0, 0, 1.1F)
        return true
    }
}