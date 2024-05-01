package com.tongtongstudio.ami.ui.insights

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.FragmentInsightsBinding
import com.tongtongstudio.ami.timer.TimerType
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class InsightsFragment : Fragment(R.layout.fragment_insights) {

    private val viewModel: InsightsViewModel by viewModels()
    private lateinit var binding: FragmentInsightsBinding
    private val combinedAchievementsData = CombinedData()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInsightsBinding.bind(view)

        setUpToolbar()
        displayInitialValues()
        displayGraphAchievements()

        // TODO: use livedata or flow 
        viewModel.updateAchievementEntries()

        viewModel.achievementsEntriesByPeriod.observe(viewLifecycleOwner) {
            //combinedAchievementsData.setData(generateLineData())
            combinedAchievementsData.setData(createBarData(it))
            binding.achievementCombinedChart.data = combinedAchievementsData
            binding.achievementCombinedChart.invalidate()
        }
    }

    private fun displayGraphAchievements() {
        binding.apply {
            achievementCombinedChart.description.isEnabled = false
            achievementCombinedChart.setDrawGridBackground(false)
            achievementCombinedChart.setDrawBarShadow(false)
            achievementCombinedChart.isHighlightFullBarEnabled = false
            achievementCombinedChart.isScaleYEnabled = false
            achievementCombinedChart.isScaleXEnabled = false
            //.setBackgroundColor(Color.WHITE)

            // draw bars behind lines
            achievementCombinedChart.drawOrder = arrayOf(
                CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE
            )


            val l = achievementCombinedChart.legend
            l.isWordWrapEnabled = true
            l.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            l.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            l.orientation = Legend.LegendOrientation.HORIZONTAL
            l.setDrawInside(false)

            val leftAxis = achievementCombinedChart.axisLeft
            leftAxis.setDrawGridLines(false)
            leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)

            val xAxis = achievementCombinedChart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTH_SIDED
            xAxis.granularity = 1f
        }
    }

    private fun displayInitialValues() {
        binding.apply {
            tvNbTasksCompleted.text = viewModel.completedTasksCount.toString()
            tvTaskAchievementRate.text =
                getString(R.string.completion_rate_value, viewModel.tasksAchievementRate)
            tvNbProjectsCompleted.text = viewModel.completedProjectsCount.toString()
            tvProjectsAchievementRate.text =
                getString(R.string.completion_rate_value, viewModel.projectsAchievementRate)
            tvEstimationTimeAccuracy.text =
                getString(R.string.completion_rate_value, viewModel.accuracyRateEstimation)
            tvOnRateCompletionTime.text =
                getString(R.string.completion_rate_value, viewModel.onTimeCompletionRate)
            tvHabitCompletionRate.text =
                getString(R.string.completion_rate_value, viewModel.habitCompletionRate)
            tvTimeWorked.text =
                TrackingTimeUtility.getFormattedWorkTime(viewModel.timeWorked, TimerType.STOPWATCH)
            tvCurrentMaxStreak.text = viewModel.ttdCurrentMaxStreak.currentStreak.toString()
            tvMaxStreak.text = viewModel.ttdMaxStreak.maxStreak.toString()
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

    private fun createLineData(entries: List<Entry>): LineData {

        val lineData = LineData()

        val set = LineDataSet(entries, "Average achievements by period")
        set.color = Color.rgb(240, 238, 70)
        set.lineWidth = 2.5f
        //set.isDrawCirclesEnabled = false
        set.fillColor = Color.rgb(240, 238, 70)
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.setDrawValues(true)
        set.valueTextSize = 10f
        set.valueTextColor = Color.rgb(240, 238, 70)

        set.axisDependency = YAxis.AxisDependency.LEFT
        lineData.addDataSet(set)

        return lineData
    }

    private fun createBarData(entries: List<BarEntry>): BarData {
        val dataSet = BarDataSet(entries, "Achievements By")
        dataSet.color = Color.rgb(60, 220, 78)
        dataSet.valueTextColor = Color.rgb(60, 220, 78)
        dataSet.valueTextSize = 10f
        dataSet.axisDependency = YAxis.AxisDependency.LEFT

        val groupSpace = 0.06F
        val barSpace = 0.02F // x2 dataset
        val barWidth = 0.45F // x2 dataset
        // (0.45 + 0.02) * 2 + 0.06 = 1.00 -> interval per "group"

        val d = BarData(dataSet)
        d.barWidth = barWidth

        // make this BarData object grouped
        //d.groupBars(0F, groupSpace, barSpace) // start at x = 0

        return d
    }

}