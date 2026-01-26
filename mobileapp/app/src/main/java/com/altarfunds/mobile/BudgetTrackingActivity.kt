package com.altarfunds.mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.databinding.ActivityBudgetTrackingBinding
import com.altarfunds.mobile.models.Budget
import com.altarfunds.mobile.ui.adapters.BudgetAdapter
import com.altarfunds.mobile.utils.CurrencyUtils
import kotlinx.coroutines.launch

class BudgetTrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetTrackingBinding
    private lateinit var budgetAdapter: BudgetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadBudgetData()
        setupClickListeners()
    }

    private fun setupUI() {
        // Set toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Budget Tracking"

        // Setup RecyclerView
        budgetAdapter = BudgetAdapter(emptyList()) { budget ->
            // Handle budget click - navigate to budget details
            Toast.makeText(this, "Clicked: ${budget.name}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerBudgets.apply {
            layoutManager = LinearLayoutManager(this@BudgetTrackingActivity)
            adapter = budgetAdapter
        }
    }

    private fun loadBudgetData() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val budgets = ApiService.getBudgets()
                updateBudgetList(budgets)
                updateBudgetSummary(budgets)
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@BudgetTrackingActivity, "Error loading budget data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateBudgetList(budgets: List<Budget>) {
        if (budgets.isEmpty()) {
            binding.recyclerBudgets.visibility = View.GONE
            binding.noBudgetsText.visibility = View.VISIBLE
        } else {
            binding.recyclerBudgets.visibility = View.VISIBLE
            binding.noBudgetsText.visibility = View.GONE
            budgetAdapter.updateData(budgets)
        }
    }

    private fun updateBudgetSummary(budgets: List<Budget>) {
        val totalAllocated = budgets.sumOf { it.amount }
        val totalSpent = budgets.sumOf { it.spent }
        val totalRemaining = totalAllocated - totalSpent
        val overallUtilization = if (totalAllocated > 0) (totalSpent * 100) / totalAllocated else 0

        binding.totalAllocated.text = CurrencyUtils.formatCurrency(totalAllocated)
        binding.totalSpent.text = CurrencyUtils.formatCurrency(totalSpent)
        binding.totalRemaining.text = CurrencyUtils.formatCurrency(totalRemaining)
        binding.overallUtilization.text = "${overallUtilization.toInt()}%"
        binding.overallUtilizationProgress.progress = overallUtilization.toInt()

        // Update status based on utilization
        binding.overallStatus.text = when {
            overallUtilization.toFloat() > 200.00 -> "Over Budget"
            overallUtilization.toFloat() > 80.0 -> "Warning"
            else -> "On Track"
        }
    }

    private fun setupClickListeners() {
        binding.fabAddBudget.setOnClickListener {
            startActivity(Intent(this, CreateBudgetActivity::class.java))
        }

        binding.btnRefresh.setOnClickListener {
            loadBudgetData()
        }
    }

    override fun onResume() {
        super.onResume()
        loadBudgetData()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
