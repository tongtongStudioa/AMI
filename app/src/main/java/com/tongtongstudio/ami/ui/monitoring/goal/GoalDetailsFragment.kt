package com.tongtongstudio.ami.ui.monitoring.goal

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.transition.MaterialContainerTransform
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import com.tongtongstudio.ami.databinding.FragmentGoalDetailsBinding
import com.tongtongstudio.ami.ui.MainActivity
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class GoalDetailsFragment : Fragment(R.layout.fragment_goal_details) {
    lateinit var binding: FragmentGoalDetailsBinding
    private val viewModel: GoalDetailsViewModel by viewModels()
    private lateinit var sharedViewModel: MainViewModel
    private lateinit var lineChart: LineChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentGoalDetailsBinding.bind(view)
        sharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.long_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
        // Shared transition id
        ViewCompat.setTransitionName(binding.goalCardView, "shared_element_${viewModel.goal?.id}")

        binding.apply {
            goalName.text = viewModel.goal?.title ?: getString(R.string.no_information)
            goalCategory.text = viewModel.category?.title
            goalCategory.isVisible = viewModel.category != null
            goalDescription.text = viewModel.goal?.description
            goalDescription.isVisible = viewModel.goal?.description != null
            goalDueDate.text = if (viewModel.goal?.dueDate != null)
                DateFormat.getDateInstance().format(viewModel.goal?.dueDate)
            else getString(R.string.no_information)
            tvGoal.text = viewModel.goal?.targetGoal.toString()
            val evaluationsAdapter = EditAttributesAdapter(object : AttributeListener<Assessment> {
                override fun onItemClicked(attribute: Assessment) {
                    //TODO: update assessment in edit assessment dialog
                }

                override fun onRemoveCrossClick(attribute: Assessment) {
                    viewModel.deleteIntermediateAssessment(attribute)
                }

            }) { binding, assessment ->
                binding.titleOverview.text = assessment.title
            }

            rvEvaluations.apply {
                adapter = evaluationsAdapter
                layoutManager = LinearLayoutManager(context)
            }
            viewModel.intermediateEvaluations?.observe(viewLifecycleOwner) {
                evaluationsAdapter.submitList(it)
                if (it.isNotEmpty()) {
                    setChartData(it)
                } else {
                    showEmptyState()
                }
            }
            lineChart = binding.intermediateEvalChart

            // completion date
            if (viewModel.goal != null) {
                val completionDateFormatted = viewModel.goal?.getFormattedDueDate()
                tvCompletionDate.text =
                    getString(R.string.completion_date, completionDateFormatted)
                tvCompletionDate.isVisible = viewModel.goal?.score != null
            } else tvCompletionDate.isVisible = false

        }

        val mainActivity = activity as MainActivity
        // imperative to see option menu and navigation icon (hamburger)
        mainActivity.setSupportActionBar(binding.toolbar)
        // retrieve app bar configuration : see MainActivity.class
        val appBarConfiguration = mainActivity.appBarConfiguration
        // to set hamburger menu work and open drawer layout
        binding.toolbar.setupWithNavController(findNavController(), appBarConfiguration)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp(mainActivity.appBarConfiguration)
        }
    }

    private fun setupLineChart() {
        // Configuration générale du graphique
        lineChart.setDrawGridBackground(false)
        lineChart.setDrawBorders(false)
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)

        // Animation
        lineChart.animateXY(1000, 1000)

        // Légende
        val legend = lineChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.textSize = 12f
        legend.textColor = R.attr.colorOnSurfaceVariant
        legend.typeface = Typeface.DEFAULT_BOLD

        // Axe X
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = R.attr.colorOutline
        xAxis.gridLineWidth = 0.5f
        xAxis.textSize = 11f
        xAxis.textColor =  R.attr.colorOnSurface
        xAxis.axisLineColor = R.attr.colorOutline
        xAxis.axisLineWidth = 1f

        // Axe Y gauche
        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = R.attr.colorOutline
        leftAxis.gridLineWidth = 0.5f
        leftAxis.textSize = 11f
        leftAxis.textColor =R.attr.colorOnSurface
        leftAxis.axisLineColor =  R.attr.colorOutline
        leftAxis.axisLineWidth = 1f
        leftAxis.axisMinimum = 0f
        lineChart.xAxis.axisMaximum = 100f // ou la valeur max de vos scores
        lineChart.xAxis.setLabelCount(6, true)
        leftAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}"
            }
        }

        // Désactiver l'axe Y droit
        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false
    }

    private fun setChartData(assessments: List<Assessment>) {
        val entries = ArrayList<Entry>()

        // Transformer les données en entrées pour le graphique
        assessments.filter { it.score != null }.sortedBy { it.dueDate }.forEachIndexed { index, assessment ->
            entries.add(Entry(index.toFloat(), assessment.score!!))
        }

        // Create dataSet
        val dataSet = LineDataSet(entries, getString(R.string.progress_assessment))

        // Customisation Material Design du dataset
        dataSet.color = R.attr.colorPrimary
        dataSet.setCircleColor(R.attr.colorPrimary)
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 6f
        dataSet.setDrawCircleHole(true)
        dataSet.circleHoleRadius = 3f
        dataSet.circleHoleColor =  R.attr.colorSurface
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = R.attr.colorOnSurface
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Courbes lisses
        dataSet.cubicIntensity = 0.2f

        // Remplissage sous la courbe (optionnel)
        //dataSet.setDrawFilled(true)
        //dataSet.fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient)

        // Formateur des valeurs
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}"
            }
        }
        // Extract date labels
        val labels = assessments.map { it.dueDate }
            .map { SimpleDateFormat(PATTERN_FORMAT_DATE, Locale.getDefault()).format(it) }

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.labelCount = entries.size

        // Créer les données finales
        val lineData = LineData(dataSet)
        lineData.setValueTypeface(Typeface.DEFAULT_BOLD)

        // Appliquer au graphique
        lineChart.data = lineData
        lineChart.invalidate() // Rafraîchir le graphique
    }

    private fun showEmptyState() {
        lineChart.clear()
        lineChart.setNoDataText("Aucune évaluation disponible")
        lineChart.setNoDataTextColor(R.attr.colorSurface)
        lineChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD)
    }

}