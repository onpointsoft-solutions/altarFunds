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
import com.altarfunds.member.adapters.DonationAdapter
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.data.mappers.toModel
import com.altarfunds.member.databinding.FragmentGivingBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GivingFragment : Fragment() {
    
    private var _binding: FragmentGivingBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var donationAdapter: DonationAdapter
    
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
        loadDonations()
    }
    
    private fun setupRecyclerView() {
        donationAdapter = DonationAdapter()
        binding.rvDonations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = donationAdapter
        }
    }
    
    private fun setupListeners() {
        binding.fabGive.setOnClickListener {
            startActivity(Intent(requireContext(), GivingActivity::class.java))
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadDonations()
        }
    }
    
    private fun loadDonations() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            // First, try to load from cache
            val cachedDonations = app.database.donationDao().getAllDonations().firstOrNull()
            if (!cachedDonations.isNullOrEmpty()) {
                val donations = cachedDonations.map { it.toModel() }
                donationAdapter.submitList(donations)
                binding.tvEmpty.gone()
                binding.rvDonations.visible()
            }
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                binding.swipeRefresh.isRefreshing = false
                if (!cachedDonations.isNullOrEmpty()) {
                    requireContext().showToast("ℹ Offline mode - Showing cached donations")
                } else {
                    requireContext().showToast("✗ No internet connection and no cached donations")
                    binding.tvEmpty.visible()
                    binding.rvDonations.gone()
                }
                return@launch
            }
            
            // Fetch from network
            try {
                val response = app.apiService.getDonations()
                
                if (response.isSuccessful && response.body() != null) {
                    val donations = response.body()!!.results
                    
                    // Cache the donations
                    if (donations.isNotEmpty()) {
                        app.database.donationDao().deleteAllDonations()
                        app.database.donationDao().insertDonations(donations.map { it.toEntity() })
                    }
                    
                    donationAdapter.submitList(donations)
                    
                    if (donations.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvDonations.gone()
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvDonations.visible()
                    }
                } else {
                    if (cachedDonations.isNullOrEmpty()) {
                        requireContext().showToast("✗ Failed to load donations")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cachedDonations.isNullOrEmpty()) {
                    requireContext().showToast("✗ Network error: ${e.message ?: "Unknown error"}")
                }
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadDonations()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
