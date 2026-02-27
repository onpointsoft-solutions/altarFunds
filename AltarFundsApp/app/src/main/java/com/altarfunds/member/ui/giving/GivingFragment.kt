package com.altarfunds.member.ui.giving

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.adapters.GivingTransactionAdapter
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.data.mappers.toModel
import com.altarfunds.member.databinding.FragmentGivingBinding
import com.altarfunds.member.models.*
import com.altarfunds.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GivingFragment : Fragment() {
    
    private var _binding: FragmentGivingBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var givingAdapter: GivingTransactionAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGivingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        loadGivingTransactions()
    }
    
    private fun setupRecyclerView() {
        givingAdapter = GivingTransactionAdapter()
        binding.rvDonations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = givingAdapter
        }
    }
    
    private fun setupListeners() {
        binding.fabGive.setOnClickListener {
            startActivity(Intent(requireContext(), GivingActivity::class.java))
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadGivingTransactions()
        }
    }
    
    private fun loadGivingTransactions() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                binding.swipeRefresh.isRefreshing = false
            requireContext().showToast("No internet connection")
                return@launch
            }
            
            // Fetch from network
            try {
                val response = app.apiService.getGivingTransactions()
                
                if (response.isSuccessful && response.body() != null) {
                    val transactions = response.body()!!.results
                    
                    givingAdapter.submitList(transactions)
                    
                    if (transactions.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvDonations.gone()
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvDonations.visible()
                    }
                } else {
                    requireContext().showToast("Failed to load giving history")
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                requireContext().showToast("Error: ${e.message}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadGivingTransactions()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
