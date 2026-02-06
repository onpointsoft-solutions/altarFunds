package com.altarfunds.member.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.MemberApp
import com.altarfunds.member.databinding.FragmentDashboardBinding
import com.altarfunds.member.ui.giving.GivingActivity
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        loadDashboardData()
    }
    
    private fun setupListeners() {
        binding.btnGiveNow.setOnClickListener {
            startActivity(Intent(requireContext(), GivingActivity::class.java))
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }
    }
    
    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDashboardStats()
                
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!
                    updateUI(stats)
                } else {
                    requireContext().showToast("Failed to load dashboard data")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("Network error")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun updateUI(stats: com.altarfunds.member.models.DashboardStats) {
        binding.tvTotalGiven.text = stats.totalDonations.formatCurrency()
        binding.tvDonationCount.text = stats.donationCount.toString()
        binding.tvAnnouncementsCount.text = stats.announcementsCount.toString()
        binding.tvDevotionalsCount.text = stats.devotionalsCount.toString()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
