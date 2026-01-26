package com.altarfunds.mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.databinding.ActivityAnalyticsBinding
import com.altarfunds.mobile.models.ChartData as AppChartData
import com.altarfunds.mobile.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadAnalyticsData()
        setupClickListeners()
    }

    private fun setupUI() {
        // Set toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Analytics & Reports"

        // Setup charts
        setupPieChart(binding.incomeBreakdownChart)
        setupPieChart(binding.expenseBreakdownChart)
        setupBarChart(binding.monthlyTrendChart)
        setupLineChart(binding.incomeExpenseChart)
    }

    private fun setupPieChart(chart: PieChart) {
        chart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(5f, 10f, 5f, 5f)
            setDrawCenterText(true)
            setCenterText("Breakdown")
            setCenterTextSize(12f)
            isRotationEnabled = false
            legend.isEnabled = true
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
            legend.setDrawInside(false)
        }
    }

    private fun setupBarChart(chart: BarChart) {
        chart.apply {
            description.isEnabled = false
            setMaxVisibleValueCount(6)
            setFitBars(true)
            legend.isEnabled = true
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            
            xAxis.apply {
                granularity = 1f
                setDrawLabels(true)
                setDrawGridLines(false)
            }
            
            axisRight.apply {
                setDrawLabels(false)
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "₦${(value / 1000000).toInt()}M"
                    }
                }
            }
        }
    }

    private fun setupLineChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            
            xAxis.apply {
                granularity = 1f
                setDrawLabels(true)
                setDrawGridLines(false)
            }
            
            axisRight.apply {
                setDrawLabels(false)
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "₦${(value / 1000000).toInt()}M"
                    }
                }
            }
        }
    }

    private fun loadAnalyticsData() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Load income breakdown
                val incomeBreakdown = ApiService.getApiInterface().getIncomeBreakdown()
                if (incomeBreakdown.isSuccessful) {
                    updateIncomeBreakdownChart(incomeBreakdown.body() ?: emptyList())
                }

                // Load expense breakdown
                val expenseBreakdown = ApiService.getApiInterface().getExpenseBreakdown()
                if (expenseBreakdown.isSuccessful) {
                    updateExpenseBreakdownChart(expenseBreakdown.body() ?: emptyList())
                }

                // Load monthly trend
                val monthlyTrend = ApiService.getApiInterface().getMonthlyTrend()
                if (monthlyTrend.isSuccessful) {
                    updateMonthlyTrendChart(monthlyTrend.body() ?: emptyList())
                    updateIncomeExpenseChart(monthlyTrend.body() ?: emptyList())
                }

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@AnalyticsActivity, "Error loading analytics data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateIncomeBreakdownChart(data: List<AppChartData>) {
        val entries = data.map { PieEntry(it.value?.toFloat() ?: 0f, it.name) }
        val dataSet = PieDataSet(entries, "Income Sources").apply {
            colors = listOf(
                getColor(R.color.holo_orange_dark),
                getColor(R.color.holo_green_dark),
                getColor(R.color.holo_blue_dark),
                getColor(R.color.holo_red_dark)
            )
            valueTextSize = 12f
            valueTextColor = getColor(R.color.white)
        }
        
        binding.incomeBreakdownChart.data = com.github.mikephil.charting.data.PieData(dataSet)
        binding.incomeBreakdownChart.invalidate()
    }

    private fun updateExpenseBreakdownChart(data: List<AppChartData>) {
        val entries = data.map { PieEntry(it.value?.toFloat() ?: 0f, it.name) }
        val dataSet = PieDataSet(entries, "Expense Categories").apply {
            colors = listOf(
                getColor(R.color.holo_red_dark),
                getColor(R.color.holo_orange_dark),
                getColor(R.color.holo_blue_dark),
                getColor(R.color.holo_purple_dark)
            )
            valueTextSize = 12f
            valueTextColor = getColor(R.color.white)
        }
        
        binding.expenseBreakdownChart.data = com.github.mikephil.charting.data.PieData(dataSet)
        binding.expenseBreakdownChart.invalidate()
    }

    private fun updateMonthlyTrendChart(data: List<AppChartData>) {
        val entries = data.map { BarEntry(it.name?.toFloat() ?: 0f, it.value?.toFloat() ?: 0f) }
        val dataSet = BarDataSet(entries, "Monthly Trend").apply {
            colors = listOf(getColor(R.color.holo_blue_dark))
            valueTextSize = 10f
        }
        
        binding.monthlyTrendChart.data = BarData(dataSet)
        binding.monthlyTrendChart.invalidate()
    }

    private fun updateIncomeExpenseChart(data: List<AppChartData>) {
        val incomeEntries = data.mapIndexed { index, chartData ->
            com.github.mikephil.charting.data.Entry(index.toFloat(), chartData.income?.toFloat() ?: 0f)
        }
        val expenseEntries = data.mapIndexed { index, chartData ->
            com.github.mikephil.charting.data.Entry(index.toFloat(), chartData.expenses?.toFloat() ?: 0f)
        }

        val incomeDataSet = com.github.mikephil.charting.data.LineDataSet(incomeEntries, "Income").apply {
            color = getColor(R.color.holo_green_dark)
            setCircleColor(getColor(R.color.holo_green_dark))
            valueTextSize = 10f
            lineWidth = 2f
            circleRadius = 4f
        }

        val expenseDataSet = com.github.mikephil.charting.data.LineDataSet(expenseEntries, "Expenses").apply {
            color = getColor(R.color.holo_red_dark)
            setCircleColor(getColor(R.color.holo_red_dark))
            valueTextSize = 10f
            lineWidth = 2f
            circleRadius = 4f
        }

        binding.incomeExpenseChart.data = LineData(incomeDataSet, expenseDataSet)
        binding.incomeExpenseChart.invalidate()
    }

    private fun setupClickListeners() {
        binding.btnExportPdf.setOnClickListener {
            Toast.makeText(this, "PDF export coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareReport.setOnClickListener {
            Toast.makeText(this, "Share report coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.fabRefresh.setOnClickListener {
            loadAnalyticsData()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
