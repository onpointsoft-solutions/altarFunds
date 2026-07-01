package com.sanctum.member.ui.giving

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sanctum.member.R
import com.sanctum.member.adapters.GivingTransactionAdapter
import com.sanctum.member.databinding.FragmentGivingBinding
import com.sanctum.member.models.BudgetSummaryResponse
import com.sanctum.member.utils.NetworkUtils
import com.sanctum.member.utils.formatCurrency
import com.sanctum.member.utils.showToast
import com.sanctum.member.viewmodel.GivingViewModel
import com.sanctum.member.viewmodel.Resource

class GivingFragment : Fragment() {

    private var _binding: FragmentGivingBinding? = null
    private val binding get() = _binding!!

    // viewModels() shares the same ViewModel instance for this Fragment's scope
    private val viewModel: GivingViewModel by viewModels()

    private lateinit var givingAdapter: GivingTransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGivingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        refresh()
    }

    private fun setupRecyclerView() {
        givingAdapter = GivingTransactionAdapter()
        givingAdapter.onPaymentCompleted = { refresh() }   // refresh after WebView returns
        binding.rvDonations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = givingAdapter
        }
    }

    private fun setupListeners() {
        binding.fabGive.setOnClickListener {
            startActivity(Intent(requireContext(), GivingActivity::class.java))
        }
        binding.swipeRefresh.setOnRefreshListener { refresh() }
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.primary),
            ContextCompat.getColor(requireContext(), R.color.secondary),
            ContextCompat.getColor(requireContext(), R.color.accent)
        )

        // "View Budget" button — prompt for PIN, then show summary
        binding.btnViewBudget.setOnClickListener { showPinDialog() }
    }

    private fun observeViewModel() {
        // Transactions → update list + summary header
        viewModel.givingTransactions.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = result is Resource.Loading
            when (result) {
                is Resource.Loading -> { /* spinner already set */ }
                is Resource.Success -> {
                    val txns = result.data
                    givingAdapter.submitList(txns)
                    binding.tvEmpty.visibility =
                        if (txns.isEmpty()) View.VISIBLE else View.GONE

                    // ── Compute giving summary ──────────────────────────
                    val total = txns.filter { it.status.lowercase() == "completed" }
                        .sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                    val currentMonth = java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
                    val monthly = txns.filter {
                        it.status.lowercase() == "completed" &&
                        it.createdAt.startsWith(currentMonth)
                    }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

                    binding.tvTotalGiving.text  = "KES ${String.format("%,.0f", total)}"
                    binding.tvMonthlyGiving.text = "KES ${String.format("%,.0f", monthly)}"
                    binding.tvTxCount.text       = "${txns.size} transaction${if (txns.size != 1) "s" else ""}"
                }
                is Resource.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    requireContext().showToast("✗ ${result.message}")
                }
            }
        }

        // Budget PIN result
        viewModel.budgetSummary.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> { /* handled by dialog's progress */ }
                is Resource.Success -> showBudgetSummaryDialog(result.data)
                is Resource.Error   -> requireContext().showToast("✗ ${result.message}")
            }
        }
    }

    private fun refresh() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            binding.swipeRefresh.isRefreshing = false
            requireContext().showToast("No internet connection")
            return
        }
        viewModel.loadGivingTransactions()
    }

    // ── Budget PIN dialog ────────────────────────────────────────────────

    private fun showPinDialog() {
        val editText = EditText(requireContext()).apply {
            hint         = "Enter 6-digit PIN"
            inputType    = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            maxEms       = 6
        }
        AlertDialog.Builder(requireContext())
            .setTitle("🔑 View Church Budget")
            .setMessage("Ask your treasurer for the budget access PIN.")
            .setView(editText)
            .setPositiveButton("Verify") { dialog, _ ->
                val pin = editText.text.toString().trim()
                if (pin.length < 4) {
                    requireContext().showToast("Please enter a valid PIN")
                } else {
                    dialog.dismiss()
                    viewModel.verifyBudgetPin(pin)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBudgetSummaryDialog(data: BudgetSummaryResponse) {
        val sb = StringBuilder()
        sb.appendLine("🏛  ${data.churchName ?: "Church"}")
        sb.appendLine("📋  ${data.pinLabel ?: "Budget Summary"}")
        sb.appendLine()
        sb.appendLine("Total Allocated : KES ${String.format("%,.0f", data.totalAllocated)}")
        sb.appendLine("Total Spent     : KES ${String.format("%,.0f", data.totalSpent)}")
        sb.appendLine("Remaining       : KES ${String.format("%,.0f", data.totalRemaining)}")

        val dept = data.byDepartment
        if (!dept.isNullOrEmpty()) {
            sb.appendLine()
            sb.appendLine("─── By Department ───")
            for (d in dept) {
                sb.appendLine(
                    "• ${d.department}: " +
                    "KES ${String.format("%,.0f", d.allocated)} allocated, " +
                    "KES ${String.format("%,.0f", d.remaining)} left"
                )
            }
        }

        data.expiresAt?.let {
            sb.appendLine()
            sb.appendLine("PIN expires: ${it.take(16).replace("T", " ")}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Church Budget Summary")
            .setMessage(sb.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
