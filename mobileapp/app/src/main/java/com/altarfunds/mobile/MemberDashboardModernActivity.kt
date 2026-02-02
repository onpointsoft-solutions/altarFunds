package com.altarfunds.mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.mobile.adapters.TransactionAdapter
import com.altarfunds.mobile.api.ApiService
import com.altarfunds.mobile.databinding.ActivityMemberDashboardModernBinding
import com.altarfunds.mobile.models.GivingTransaction
import com.altarfunds.mobile.utils.CurrencyUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MemberDashboardModernActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberDashboardModernBinding
    private lateinit var transactionAdapter: TransactionAdapter

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
        binding.progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Load user profile
                loadUserProfile()
                
                // Load financial summary
                loadFinancialSummary()
                
                // Load recent transactions
                loadRecentTransactions()

                binding.progressBar?.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar?.visibility = View.GONE
                showError("Error loading data: ${e.message}")
            }
        }
    }

    private suspend fun loadUserProfile() {
        try {
            val response = ApiService.getApiInterface().getProfile()
            if (response.isSuccessful) {
                val user = response.body()
                binding.userName?.text = "${user?.first_name ?: ""} ${user?.last_name ?: ""}".trim()
                    .ifEmpty { user?.email ?: "User" }
                binding.welcomeText?.text = "Welcome back, ${user?.first_name ?: "Member"}!"
                
                // Display church information if available
                user?.church_name?.let { churchName ->
                    binding.userName?.text = "${binding.userName?.text}\n$churchName"
                }
            }
        } catch (e: Exception) {
            // Set default values on error
            binding.userName?.text = "Member"
            binding.welcomeText?.text = "Welcome back!"
        }
    }

    private suspend fun loadFinancialSummary() {
        try {
            val response = ApiService.getApiInterface().getFinancialSummaryBackend()
            if (response.isSuccessful && response.body()?.success == true) {
                val summary = response.body()?.data
                
                val totalIncome = summary?.total_income ?: 0.0
                val totalExpenses = summary?.total_expenses ?: 0.0
                val netBalance = summary?.net_income ?: (totalIncome - totalExpenses)
                
                // Display financial values
                binding.totalIncome?.text = CurrencyUtils.formatCurrency(totalIncome)
                binding.totalExpenses?.text = CurrencyUtils.formatCurrency(totalExpenses)
                binding.netBalance?.text = CurrencyUtils.formatCurrency(netBalance)
                
                // Calculate and display trends (mock calculation - replace with actual API data)
                val incomeTrend = calculateTrend(totalIncome)
                val expenseTrend = calculateTrend(totalExpenses)
                
                binding.incomeTrend?.text = formatTrend(incomeTrend)
                binding.expenseTrend?.text = formatTrend(expenseTrend)
                
                // Display balance status
                binding.balanceStatus?.text = when {
                    netBalance > 0 -> "Positive cash flow - You're doing great!"
                    netBalance < 0 -> "Negative balance - Review your expenses"
                    else -> "Balanced - Income equals expenses"
                }
                
                // Display additional statistics if available
                // Note: total_givings and monthly_average fields not in API response
            }
        } catch (e: Exception) {
            // Show default values on error
            binding.totalIncome?.text = CurrencyUtils.formatCurrency(0.0)
            binding.totalExpenses?.text = CurrencyUtils.formatCurrency(0.0)
            binding.netBalance?.text = CurrencyUtils.formatCurrency(0.0)
            binding.incomeTrend?.text = "+0%"
            binding.expenseTrend?.text = "+0%"
            binding.balanceStatus?.text = "Loading financial data..."
        }
    }
    
    private fun calculateTrend(value: Double): Double {
        // Mock trend calculation - in production, this should come from API
        return if (value > 0) (5..15).random().toDouble() else 0.0
    }
    
    private fun formatTrend(trend: Double): String {
        val sign = if (trend >= 0) "+" else ""
        return "$sign${String.format("%.1f", trend)}%"
    }

    private suspend fun loadRecentTransactions() {
        try {
            val response = ApiService.getApiInterface().getGivingHistoryBackend()
            if (response.isSuccessful && response.body()?.success == true) {
                val history = response.body()?.data
                val allTransactions = history?.givings ?: emptyList()
                val recentTransactions = allTransactions.take(5)
                
                if (recentTransactions.isEmpty()) {
                    binding.recyclerRecentTransactions?.visibility = View.GONE
                    binding.noTransactionsText?.visibility = View.VISIBLE
                    binding.noTransactionsText?.text = "No transactions yet. Start giving to see your history here."
                } else {
                    binding.recyclerRecentTransactions?.visibility = View.VISIBLE
                    binding.noTransactionsText?.visibility = View.GONE
                    transactionAdapter.submitList(recentTransactions as List<GivingTransaction?>?)
                    
                    // Display transaction statistics
                    val totalCount = allTransactions.size
                    val totalAmount = allTransactions.sumOf { it?.amount ?: 0.0 }
                    
                    // Update header with transaction count if view exists
                    // Note: txtTransactionCount and txtTransactionTotal views not in current layout
                }
            } else {
                showEmptyTransactions("Unable to load transactions. Please try again.")
            }
        } catch (e: Exception) {
            showEmptyTransactions("Error loading transactions: ${e.message}")
        }
    }
    
    private fun showEmptyTransactions(message: String) {
        binding.recyclerRecentTransactions?.visibility = View.GONE
        binding.noTransactionsText?.visibility = View.VISIBLE
        binding.noTransactionsText?.text = message
    }
    
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun setupClickListeners() {
        // New Giving Button
        binding.btnNewGiving.setOnClickListener {
            startActivity(Intent(this, NewGivingActivity::class.java))
        }

        // New Pledge Button - Note: btnNewPledge not in current layout

        // View Reports Button
        binding.btnViewReports.setOnClickListener {
            // Navigate to reports activity (to be created)
            Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // View All Transactions
        binding.btnViewAll.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }
        
        // View Pledges - Note: btnViewPledges not in current layout
    }
    
    private fun createNewPledge() {
        // Navigate to pledge creation activity
        val intent = Intent(this, NewGivingActivity::class.java)
        startActivity(intent)
    }
    
    private fun submitPledge(amount: Double, purpose: String) {
        lifecycleScope.launch {
            try {
                val pledgeRequest = com.altarfunds.mobile.models.PledgeRequest(
                    amount = amount,
                    description = purpose,
                    pledge_type = "financial"
                )
                
                val response = com.altarfunds.mobile.api.ApiService.getApiInterface().createPledge(pledgeRequest)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@MemberDashboardModernActivity, "Pledge created successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MemberDashboardModernActivity, "Failed to create pledge", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MemberDashboardModernActivity, "Error creating pledge: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun viewPledges() {
        lifecycleScope.launch {
            try {
                val response = com.altarfunds.mobile.api.ApiService.getApiInterface().getPledges()
                
                if (response.isSuccessful) {
                    val pledges = response.body()?.pledges ?: emptyList()
                    showPledgesDialog(pledges)
                } else {
                    Toast.makeText(this@MemberDashboardModernActivity, "Failed to load pledges", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MemberDashboardModernActivity, "Error loading pledges: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showPledgesDialog(pledges: List<Any>) {
        if (pledges.isEmpty()) {
            Toast.makeText(this, "No pledges found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val pledgeText = pledges.joinToString("\n\n") { pledge ->
            // Format pledge information - adjust based on actual Pledge model structure
            "Pledge: ${pledge.toString()}"
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Your Pledges")
            .setMessage(pledgeText)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
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
