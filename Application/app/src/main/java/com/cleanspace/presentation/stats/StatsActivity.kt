package com.cleanspace.presentation.stats

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.cleanspace.databinding.ActivityBaseBinding
import com.cleanspace.databinding.ActivityStatsBinding
import com.cleanspace.domain.utils.FileSizeFormatter
import com.cleanspace.presentation.common.BaseActivity
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatsActivity : BaseActivity(com.cleanspace.R.layout.activity_base) {

    private lateinit var baseBinding: ActivityBaseBinding
    private lateinit var binding: ActivityStatsBinding

    private val viewModel: StatsViewModel by viewModels()

    private val calmPalette = listOf(
        Color.parseColor("#6B8E7A"), // sage
        Color.parseColor("#7A8FB5"), // soft indigo
        Color.parseColor("#B58A7A"), // dusty terracotta
        Color.parseColor("#8A9F7A"), // olive pastel
        Color.parseColor("#9A86B8"), // muted violet
        Color.parseColor("#7FA6A3"), // soft teal
    )

    private val categoryColors = mapOf(
        "Фото" to Color.parseColor("#7A8FB5"),
        "Дублікати" to Color.parseColor("#9A86B8"),
        "Аудіо/Відео" to Color.parseColor("#7FA6A3"),
        "Кеш телефону" to Color.parseColor("#8A9F7A"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseBinding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(baseBinding.root)

        binding = ActivityStatsBinding.inflate(layoutInflater, baseBinding.contentContainer, true)
        setupBottomNavigation(baseBinding.bottomNavigation)

        setupCharts()
        observeData()
    }

    private fun setupCharts() {
        binding.lineChart.apply {
            description = Description().apply { text = "" }
            setNoDataText("Немає даних")
            axisRight.isEnabled = false
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
        }

        binding.pieChart.apply {
            description = Description().apply { text = "" }
            setNoDataText("Немає даних")
            setUsePercentValues(false)
            setDrawEntryLabels(false)
            legend.isEnabled = true
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.records.collectLatest { records ->
                binding.tvSummary.text = getString(com.cleanspace.R.string.stats_records_count, records.size)

                val weekly = StatsChartHelper.buildWeeklyFreedSeries(
                    nowMs = System.currentTimeMillis(),
                    records = records,
                )

                val set = LineDataSet(weekly.entries, "Звільнено (MB/день)").apply {
                    color = calmPalette.first()
                    lineWidth = 2f
                    setDrawCircles(true)
                    setCircleColor(calmPalette[1])
                    valueTextSize = 10f
                    valueTextColor = Color.parseColor("#4F5D73")
                }
                binding.lineChart.data = LineData(set)
                binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(weekly.xLabels)
                binding.lineChart.invalidate()
            }
        }

        lifecycleScope.launch {
            viewModel.pieEntries.collectLatest { pieEntries ->
                if (pieEntries.isEmpty()) {
                    binding.pieChart.clear()
                    binding.pieChart.invalidate()
                } else {
                    val pieSet = PieDataSet(pieEntries, "Категорії").apply {
                        colors = pieEntries.map { entry ->
                            categoryColors[entry.label] ?: calmPalette.last()
                        }
                        valueTextSize = 10f
                        valueTextColor = Color.parseColor("#3F4652")
                    }
                    binding.pieChart.data = PieData(pieSet)
                    binding.pieChart.invalidate()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.totalFreedBytes.collectLatest { total ->
                binding.tvTotalFreed.text = getString(
                    com.cleanspace.R.string.stats_total_freed,
                    FileSizeFormatter.formatBytes(total),
                )
            }
        }
    }
}
