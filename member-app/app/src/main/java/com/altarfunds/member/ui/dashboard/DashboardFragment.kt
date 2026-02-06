package com.altarfunds.member.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.adapters.AnnouncementAdapter
import com.altarfunds.member.adapters.DevotionalAdapter
import com.altarfunds.member.databinding.FragmentDashboardBinding
import com.altarfunds.member.ui.giving.GivingActivity
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var announcementAdapter: AnnouncementAdapter
    private lateinit var devotionalAdapter: DevotionalAdapter
    
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
        
        setupRecyclerViews()
        setupListeners()
        loadDashboardData()
    }
    
    private fun setupRecyclerViews() {
        announcementAdapter = AnnouncementAdapter()
        binding.rvRecentAnnouncements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = announcementAdapter
        }
        
        devotionalAdapter = DevotionalAdapter()
        binding.rvRecentDevotionals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = devotionalAdapter
        }
    }
    
    private fun setupListeners() {
        binding.btnGiveNow.setOnClickListener {
            startActivity(Intent(requireContext(), GivingActivity::class.java))
        }
        
        binding.btnViewAllAnnouncements.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                com.altarfunds.member.R.id.bottomNav
            )?.selectedItemId = com.altarfunds.member.R.id.nav_announcements
        }
        
        binding.btnViewAllDevotionals.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                com.altarfunds.member.R.id.bottomNav
            )?.selectedItemId = com.altarfunds.member.R.id.nav_devotionals
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }
    }
    
    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val statsResponse = app.apiService.getDashboardStats()
                
                if (statsResponse.isSuccessful && statsResponse.body() != null) {
                    val stats = statsResponse.body()!!
                    updateUI(stats)
                }
                
                loadRecentAnnouncements()
                loadRecentDevotionals()
                
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("Network error")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun loadRecentAnnouncements() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getAnnouncements(page = 1, pageSize = 3)
                
                if (response.isSuccessful && response.body() != null) {
                    val announcements = response.body()!!.results
                    
                    if (announcements.isEmpty()) {
                        binding.rvRecentAnnouncements.gone()
                        binding.tvEmptyAnnouncements.visible()
                    } else {
                        binding.rvRecentAnnouncements.visible()
                        binding.tvEmptyAnnouncements.gone()
                        announcementAdapter.submitList(announcements)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun loadRecentDevotionals() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionals(page = 1, pageSize = 3)
                
                if (response.isSuccessful && response.body() != null) {
                    val devotionals = response.body()!!.results
                    
                    if (devotionals.isEmpty()) {
                        binding.rvRecentDevotionals.gone()
                        binding.tvEmptyDevotionals.visible()
                    } else {
                        binding.rvRecentDevotionals.visible()
                        binding.tvEmptyDevotionals.gone()
                        devotionalAdapter.submitList(devotionals)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
