package com.tongtongstudio.ami.ui.insights

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import com.tongtongstudio.ami.data.datatables.TimeWorkedDistribution
import com.tongtongstudio.ami.data.datatables.TtdAchieved
import com.tongtongstudio.ami.databinding.FragmentInsightsBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility
import com.tongtongstudio.ami.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class InsightsFragment : Fragment(R.layout.fragment_insights) {

    private val viewModel: InsightsViewModel by viewModels()
    private lateinit var binding: FragmentInsightsBinding
    private val combinedAchievementsData = CombinedData()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentInsightsBinding.bind(view)

        setUpToolbar()

        initCombinedChart()
        initPieChart()

        initPopUpMenuCategory(binding.btnChangeCategory1)
        initPopUpMenuCategory(binding.btnChangeCategory2)

        viewModel.tasksAchievementRate.observe(viewLifecycleOwner) {
            binding.tvTaskAchievementRate.text = if (it != null)
                getString(
                    R.string.completion_rate_value,
                    it
                ) else getString(R.string.no_information)
        }
        viewModel.completedTasksCount.observe(viewLifecycleOwner) {
            binding.tvNbTasksCompleted.text = it.toString()
        }
        viewModel.projectsAchievementRate.observe(viewLifecycleOwner) {
            binding.tvProjectsAchievementRate.text = if (it != null)
                getString(
                    R.string.completion_rate_value,
                    it
                ) else getString(R.string.no_information)
        }
        viewModel.completedProjectsCount.observe(viewLifecycleOwner) {
            binding.tvNbProjectsCompleted.text = it.toString()
        }
        viewModel.timeWorked.observe(viewLifecycleOwner) {
            binding.tvTimeWorked.text = TrackingTimeUtility.getFormattedTimeWorked(it)
        }
        viewModel.accuracyRateEstimation.observe(viewLifecycleOwner) {
            binding.tvEstimationTimeAccuracy.text =
                getString(R.string.completion_rate_value, it)
        }
        viewModel.onTimeCompletionRate.observe(viewLifecycleOwner) {
            binding.tvOnRateCompletionTime.text =
                getString(R.string.completion_rate_value, it)
        }
        viewModel.ttdCurrentMaxStreak.observe(viewLifecycleOwner) {
            binding.tvCurrentMaxStreak.text =
                it?.streakInfo?.toString() ?: getString(R.string.no_information)
        }
        viewModel.ttdMaxStreak.observe(viewLifecycleOwner) {
            binding.tvMaxStreak.text =
                it?.streakInfo?.toString() ?: getString(R.string.no_information)
        }
        viewModel.habitCompletionRate.observe(viewLifecycleOwner) {
            binding.tvHabitCompletionRate.text = if (it != null)
                getString(
                    R.string.completion_rate_value,
                    it
                ) else getString(R.string.no_information)
        }
        viewModel.achievementsByPeriod.observe(viewLifecycleOwner) {
            if (it != null)
                showCombinedChart(it)
        }
        viewModel.timeWorkedDistribution.observe(viewLifecycleOwner) {
            showPieChart(it)
        }
    }

    private fun initPopUpMenuCategory(button: ImageButton) {
        val dropDownMenu = PopupMenu(context, button)
        viewModel.categories.observe(viewLifecycleOwner) {
            dropDownMenu.menu.add("All")
            for (category in it) {
                dropDownMenu.menu.add(category.title)
            }
        }
        button.setOnClickListener {
            dropDownMenu.show()
        }
        dropDownMenu.setOnMenuItemClickListener {
            viewModel.updateCategoryId(it.title.toString())
            true
        }
    }

    private fun initCombinedChart() {
        binding.apply {
            achievementCombinedChart.description.isEnabled = false
            achievementCombinedChart.setDrawGridBackground(false)
            achievementCombinedChart.setDrawBarShadow(false)

            // Remove grid lines and disabled scaling
            achievementCombinedChart.xAxis.setDrawGridLines(false)
            achievementCombinedChart.axisLeft.setDrawGridLines(true)
            achievementCombinedChart.axisRight.setDrawGridLines(false)
            achievementCombinedChart.isScaleYEnabled = false
            achievementCombinedChart.isScaleXEnabled = false

            // Set custom X-axis
            val xAxis = binding.achievementCombinedChart.xAxis
            xAxis.labelRotationAngle = -45F
            //xAxis.valueFormatter = SimpleDateFormatter()
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 7F

            // Hide legend
            binding.achievementCombinedChart.legend.isEnabled = false
        }
    }

    private fun initBarDataSet(barDataSet: BarDataSet) {
        barDataSet.setDrawValues(true)
        barDataSet.color = resources.getColor(R.color.french_blue)
    }

    private fun showCombinedChart(listTtdAchieved: List<TtdAchieved?>) {
        // Filter out null entries and map to BarEntry
        val entries = listTtdAchieved.mapIndexedNotNull { index, value ->
            value?.let { BarEntry(index.toFloat(), it.completedCount) }
        }
        // Extract date labels
        val labels = listTtdAchieved.mapNotNull { it?.completionDate }
            .map { SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(it) }

        binding.achievementCombinedChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.achievementCombinedChart.xAxis.labelCount = entries.size
        val barDataSet = BarDataSet(entries, "")
        initBarDataSet(barDataSet)
        val barData = BarData(barDataSet)
        barData.barWidth = 0.25F
        // Animate chart
        binding.achievementCombinedChart.animateY(1200, Easing.EaseInOutQuad)
        binding.achievementCombinedChart.data = barData
        binding.achievementCombinedChart.invalidate()
    }

    private fun showPieChart(listTimeWorkedDistribution: List<TimeWorkedDistribution>) {
        val pieEntries = viewModel.getTimeWorkedDistributionEntries(listTimeWorkedDistribution)

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
        colors.add(Color.parseColor("#304567"))
        colors.add(Color.parseColor("#309967"))
        colors.add(Color.parseColor("#476567"))
        colors.add(Color.parseColor("#890567"))
        colors.add(Color.parseColor("#a35567"))
        colors.add(Color.parseColor("#ff5f67"))
        colors.add(Color.parseColor("#3ca567"))


        //collecting the entries with label name
        val pieDataSet = PieDataSet(pieEntries, "Distribution of time worked")
        //setting text size of the value
        pieDataSet.valueTextSize = 7f
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        //grouping the data set from entry to chart
        val pieData = PieData(pieDataSet)
        //showing the value of the entries, default true if not set
        pieData.setDrawValues(true)
        pieData.setValueFormatter(PercentFormatter(binding.pieChart))
        //adding animation so the entries pop up from 0 degree
        binding.pieChart.animateY(1200, Easing.EaseInOutQuad)
        binding.pieChart.data = pieData
        binding.pieChart.invalidate()
    }

    private fun initPieChart() {
        binding.apply {
            //using percentage as values instead of amount
            pieChart.setUsePercentValues(true)

            //remove the description label on the lower left corner, default true if not set
            pieChart.description.isEnabled = false

            //enabling the user to rotate the chart, default true
            binding.pieChart.isRotationEnabled = false
            //adding friction when rotating the pie chart
            pieChart.dragDecelerationFrictionCoef = 0.9f
            //setting the first entry start from right hand side, default starting from top
            pieChart.rotationAngle = 0F

            //highlight the entry when it is tapped, default true if not set
            pieChart.isHighlightPerTapEnabled = true
            //setting the color of the hole in the middle, default white
            pieChart.setHoleColor(Color.TRANSPARENT)
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
}