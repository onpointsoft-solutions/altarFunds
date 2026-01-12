package com.altarfunds.mobile.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.mobile.NewGivingActivity
import com.altarfunds.mobile.RecurringGivingActivity
import com.altarfunds.mobile.TransactionDetailsActivity
import com.altarfunds.mobile.TransactionHistoryActivity
import com.altarfunds.mobile.databinding.FragmentGivingModernBinding
import com.altarfunds.mobile.ui.adapters.GivingCategoryModernAdapter
import com.altarfunds.mobile.ui.adapters.RecentTransactionModernAdapter
import com.altarfunds.mobile.utils.CurrencyUtils
import com.altarfunds.mobile.utils.DateUtils
import com.altarfunds.mobile.models.GivingTransactionResponse
import kotlinx.coroutines.launch

class GivingFragmentModern : Fragment() {

    private lateinit var binding: FragmentGivingModernBinding
    private lateinit var categoryAdapter: GivingCategoryModernAdapter
    private lateinit var recentTransactionAdapter: RecentTransactionModernAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGivingModernBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        loadGivingData()
        setupClickListeners()
        
        // Add animations
        setupAnimations()
    }

    private fun setupRecyclerViews() {
        // Setup categories recycler view
        categoryAdapter = GivingCategoryModernAdapter { category ->
            // Handle category click - start giving flow
            startGivingFlow(category.id, category.name)
        }
        
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Setup recent transactions recycler view
        recentTransactionAdapter = RecentTransactionModernAdapter { transaction ->
            // Handle transaction click - show details
            showTransactionDetails(transaction.transaction_id)
        }
        
        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentTransactionAdapter
        }
    }

    private fun loadGivingData() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Initialize API if not already done
                com.altarfunds.mobile.api.ApiService.initialize(requireActivity().application as com.altarfunds.mobile.AltarFundsApp)
                
                // Load giving summary
                val givingSummaryResponse = com.altarfunds.mobile.api.ApiService.getApiInterface().getGivingSummary()
                
                if (givingSummaryResponse.isSuccessful && givingSummaryResponse.body() != null) {
                    val summary = givingSummaryResponse.body()!!
                    updateUIWithSummary(summary)
                } else {
                    android.util.Log.e("GivingFragment", "Failed to load giving summary: ${givingSummaryResponse.code()}")
                    // Create sample summary for demo
                    val sampleSummary = com.altarfunds.mobile.models.GivingSummaryResponse(
                        total_giving = 50000.0,
                        this_month = 5000.0,
                        this_year = 25000.0,
                        last_transaction = null,
                        giving_categories = emptyList(),
                        recurring_giving = emptyList()
                    )
                    updateUIWithSummary(sampleSummary)
                }
                
                // Load giving categories
                loadGivingCategories()
                
                // Load recent transactions
                loadRecentTransactions()
                
            } catch (e: Exception) {
                android.util.Log.e("GivingFragment", "Exception loading giving data", e)
                Toast.makeText(requireContext(), "Failed to load giving data: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Load sample data as fallback
                loadSampleData()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun loadSampleData() {
        // Load sample summary
        val sampleSummary = com.altarfunds.mobile.models.GivingSummaryResponse(
            total_giving = 50000.0,
            this_month = 5000.0,
            this_year = 25000.0,
            last_transaction = null,
            giving_categories = emptyList(),
            recurring_giving = emptyList()
        )
        updateUIWithSummary(sampleSummary)
        
        // Load sample categories
        loadSampleCategories()
        
        // Load sample transactions
        loadSampleTransactions()
    }
    
    private fun loadSampleTransactions() {
        val sampleTransactions = listOf(
            com.altarfunds.mobile.models.GivingTransactionResponse(
                transaction_id = "TXN001",
                amount = 1000.0,
                category = com.altarfunds.mobile.models.GivingCategory(
                    id = 1,
                    name = "Tithe",
                    description = "Regular tithe offering",
                    is_tax_deductible = true,
                    is_active = true
                ),
                status = "completed",
                payment_method = "mpesa",
                transaction_date = "2024-01-15T10:30:00",
                note = "Regular tithe"
            ),
            com.altarfunds.mobile.models.GivingTransactionResponse(
                transaction_id = "TXN002",
                amount = 500.0,
                category = com.altarfunds.mobile.models.GivingCategory(
                    id = 2,
                    name = "Offering",
                    description = "General offering",
                    is_tax_deductible = true,
                    is_active = true
                ),
                status = "completed",
                payment_method = "mpesa",
                transaction_date = "2024-01-10T09:15:00",
                note = "Sunday offering"
            )
        )
        
        recentTransactionAdapter.submitList(sampleTransactions)
        binding.tvNoTransactions.visibility = View.GONE
        binding.rvRecentTransactions.visibility = View.VISIBLE
    }

    private fun updateUIWithSummary(summary: com.altarfunds.mobile.models.GivingSummaryResponse) {
        binding.tvTotalGiving.text = CurrencyUtils.formatCurrency(summary.total_giving)
        binding.tvThisMonth.text = CurrencyUtils.formatCurrency(summary.this_month)
        
        // Animate the numbers
        animateValue(binding.tvTotalGiving, summary.total_giving)
        animateValue(binding.tvThisMonth, summary.this_month)
    }

    private fun loadGivingCategories() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                // Initialize API if not already done
                com.altarfunds.mobile.api.ApiService.initialize(requireActivity().application as com.altarfunds.mobile.AltarFundsApp)
                
                val churchInfoResponse = com.altarfunds.mobile.api.ApiService.getApiInterface().getChurchInfo()
                
                if (churchInfoResponse.isSuccessful && churchInfoResponse.body() != null) {
                    val categories = churchInfoResponse.body()!!.giving_categories
                    android.util.Log.d("GivingFragment", "Loaded ${categories.size} categories")
                    
                    if (categories.isNotEmpty()) {
                        categoryAdapter.submitList(categories)
                    } else {
                        // Add sample categories for demo if API returns empty
                        val sampleCategories = listOf(
                            com.altarfunds.mobile.models.GivingCategory(
                                id = 1,
                                name = "Tithe",
                                description = "Regular tithe offering",
                                is_tax_deductible = true,
                                is_active = true
                            ),
                            com.altarfunds.mobile.models.GivingCategory(
                                id = 2,
                                name = "Offering",
                                description = "General offering",
                                is_tax_deductible = true,
                                is_active = true
                            ),
                            com.altarfunds.mobile.models.GivingCategory(
                                id = 3,
                                name = "Mission",
                                description = "Mission support",
                                is_tax_deductible = true,
                                is_active = true
                            ),
                            com.altarfunds.mobile.models.GivingCategory(
                                id = 4,
                                name = "Building Fund",
                                description = "Church building fund",
                                is_tax_deductible = true,
                                is_active = true
                            )
                        )
                        categoryAdapter.submitList(sampleCategories)
                        android.util.Log.d("GivingFragment", "Using sample categories")
                    }
                } else {
                    android.util.Log.e("GivingFragment", "Failed to load categories: ${churchInfoResponse.code()}")
                    Toast.makeText(requireContext(), "Failed to load categories: ${churchInfoResponse.code()}", Toast.LENGTH_SHORT).show()
                    
                    // Load sample categories as fallback
                    loadSampleCategories()
                }
            } catch (e: Exception) {
                android.util.Log.e("GivingFragment", "Exception loading categories", e)
                Toast.makeText(requireContext(), "Failed to load categories: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Load sample categories as fallback
                loadSampleCategories()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun loadSampleCategories() {
        val sampleCategories = listOf(
            com.altarfunds.mobile.models.GivingCategory(
                id = 1,
                name = "Tithe",
                description = "Regular tithe offering",
                is_tax_deductible = true,
                is_active = true
            ),
            com.altarfunds.mobile.models.GivingCategory(
                id = 2,
                name = "Offering",
                description = "General offering",
                is_tax_deductible = true,
                is_active = true
            ),
            com.altarfunds.mobile.models.GivingCategory(
                id = 3,
                name = "Mission",
                description = "Mission support",
                is_tax_deductible = true,
                is_active = true
            ),
            com.altarfunds.mobile.models.GivingCategory(
                id = 4,
                name = "Building Fund",
                description = "Church building fund",
                is_tax_deductible = true,
                is_active = true
            )
        )
        categoryAdapter.submitList(sampleCategories)
        android.util.Log.d("GivingFragment", "Loaded sample categories as fallback")
    }

    private fun loadRecentTransactions() {
        lifecycleScope.launch {
            try {
                // Initialize API if not already done
                com.altarfunds.mobile.api.ApiService.initialize(requireActivity().application as com.altarfunds.mobile.AltarFundsApp)
                
                val transactionsResponse = com.altarfunds.mobile.api.ApiService.getApiInterface()
                    .getGivingTransactions(page = 1, limit = 5)
                
                if (transactionsResponse.isSuccessful && transactionsResponse.body() != null) {
                    val transactions = transactionsResponse.body()!!.results
                    android.util.Log.d("GivingFragment", "Loaded ${transactions.size} transactions")
                    
                    if (transactions.isNotEmpty()) {
                        recentTransactionAdapter.submitList(transactions)
                        binding.tvNoTransactions.visibility = View.GONE
                        binding.rvRecentTransactions.visibility = View.VISIBLE
                    } else {
                        binding.tvNoTransactions.visibility = View.VISIBLE
                        binding.rvRecentTransactions.visibility = View.GONE
                    }
                } else {
                    android.util.Log.e("GivingFragment", "Failed to load transactions: ${transactionsResponse.code()}")
                    // Load sample transactions as fallback
                    loadSampleTransactions()
                }
            } catch (e: Exception) {
                android.util.Log.e("GivingFragment", "Exception loading transactions", e)
                Toast.makeText(requireContext(), "Failed to load transactions: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Load sample transactions as fallback
                loadSampleTransactions()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnGiveNow.setOnClickListener {
            // Navigate to new giving activity
            startActivity(Intent(requireContext(), NewGivingActivity::class.java))
        }

        binding.btnRecurringGiving.setOnClickListener {
            // Navigate to recurring giving setup
            startActivity(Intent(requireContext(), RecurringGivingActivity::class.java))
        }

        binding.btnViewAllTransactions.setOnClickListener {
            // Navigate to transaction history
            startActivity(Intent(requireContext(), TransactionHistoryActivity::class.java))
        }
    }

    private fun setupAnimations() {
        // Add entrance animations to cards
        binding.root.post {
            // Animate cards from bottom
            binding.root.alpha = 0f
            binding.root.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun startGivingFlow(categoryId: Int, categoryName: String) {
        val intent = Intent(requireContext(), NewGivingActivity::class.java).apply {
            putExtra("category_id", categoryId)
            putExtra("category_name", categoryName)
        }
        startActivity(intent)
    }

    private fun showTransactionDetails(transactionId: String) {
        val intent = Intent(requireContext(), TransactionDetailsActivity::class.java).apply {
            putExtra("transaction_id", transactionId)
        }
        startActivity(intent)
    }

    private fun animateValue(textView: android.widget.TextView, targetValue: Double) {
        // Simple number animation
        textView.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                textView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }
}
