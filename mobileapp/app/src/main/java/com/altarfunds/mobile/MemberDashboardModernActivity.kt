package com.altarfunds.mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.mobile.adapters.TransactionAdapter
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.databinding.ActivityMemberDashboardModernBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MemberDashboardModernActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberDashboardModernBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "NG"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberDashboardModernBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadDashboardData()
        setupClickListeners()
    }

    private fun setupUI() {
        // Set toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Financial Dashboard"

        // Setup RecyclerView
        transactionAdapter = TransactionAdapter()
        binding.recyclerRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@MemberDashboardModernActivity)
            adapter = transactionAdapter
        }

        // Setup Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Already on dashboard - show dashboard content
                    showDashboardContent()
                }
                R.id.nav_giving -> {
                    // Navigate to giving activity/fragment
                    startActivity(Intent(this, NewGivingActivity::class.java))
                }
                R.id.nav_churches -> {
                    // Navigate to churches
                    startActivity(Intent(this, ChurchSearchActivity::class.java))
                }
                R.id.nav_devotionals -> {
                    // Navigate to devotionals
                    startActivity(Intent(this, DevotionalsActivity::class.java))
                }
                R.id.nav_profile -> {
                    // Navigate to profile
                    startActivity(Intent(this, EditProfileActivity::class.java))
                }
            }
            true
        }

        // View All Transactions
        binding.btnViewAll.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
    }

    private fun showDashboardContent() {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Load financial summary
                val financialSummary = ApiService.getFinancialSummary()
                updateFinancialSummary(financialSummary)

                // Load recent transactions
                val transactions = ApiService.getRecentTransactions(limit = 5)
                updateRecentTransactions(transactions)

                // Load user profile
                val userProfile = ApiService.getUserProfile()
                updateUserInfo(userProfile)

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Log.e("Dashboard", "Error loading data", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@MemberDashboardModernActivity, "Error loading data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFinancialSummary(summary: FinancialSummary) {
        binding.totalIncome.text = CurrencyUtils.formatCurrency(summary.totalIncome)
        binding.totalExpenses.text = CurrencyUtils.formatCurrency(summary.totalExpenses)
        binding.netBalance.text = CurrencyUtils.formatCurrency(summary.netIncome)
        
        // Update balance status
        binding.balanceStatus.text = if (summary.netIncome >= 0) {
            "Positive cash flow"
        } else {
            "Negative cash flow"
        }

        // Update trends (mock data for now, can be calculated from historical data)
        binding.incomeTrend.text = "+15%"
        binding.expenseTrend.text = "+8%"
    }

    private fun updateRecentTransactions(transactions: List<GivingTransactionResponse>) {
        if (transactions.isEmpty()) {
            binding.recyclerRecentTransactions.visibility = View.GONE
            binding.noTransactionsText.visibility = View.VISIBLE
        } else {
            binding.recyclerRecentTransactions.visibility = View.VISIBLE
            binding.noTransactionsText.visibility = View.GONE
            transactionHistoryAdapter.updateData(transactions)
        }
    }

    private fun updateUserInfo(user: User) {
        binding.userName.text = user.first_name
        binding.welcomeText.text = "Welcome back, ${user.first_name.split(" ").first()}!"
    }

    private fun setupClickListeners() {
        // New Giving FAB
        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(this, NewGivingActivity::class.java))
        }

        // New Giving Button
        binding.btnNewGiving.setOnClickListener {
            startActivity(Intent(this, NewGivingActivity::class.java))
        }

        // View Reports Button
        binding.btnViewReports.setOnClickListener {
            // Navigate to reports activity (to be created)
            Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // View All Transactions
        binding.btnViewAll.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when activity resumes
        loadDashboardData()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
