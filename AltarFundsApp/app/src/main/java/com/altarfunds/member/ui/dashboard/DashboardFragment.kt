package com.altarfunds.member.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.altarfunds.member.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.adapters.AnnouncementAdapter
import com.altarfunds.member.adapters.DevotionalAdapter
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.data.mappers.toModel
import com.altarfunds.member.databinding.FragmentDashboardBinding
import com.altarfunds.member.ui.giving.GivingActivity
import com.altarfunds.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
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
        
        // Customize swipe refresh colors
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.primary),
            ContextCompat.getColor(requireContext(), R.color.secondary),
            ContextCompat.getColor(requireContext(), R.color.accent)
        )
    }
    
    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            // Clear stale cache first to ensure fresh data
            app.database.announcementDao().deleteAllAnnouncements()
            app.database.devotionalDao().deleteAllDevotionals()
            app.database.dashboardStatsDao().deleteDashboardStats()
            
            // Check network availability
            val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
            
            if (!isOnline) {
                binding.swipeRefresh.isRefreshing = false
                requireContext().showToast("✗ No internet connection")
                // Show empty state when offline and no cached data
                _binding?.rvRecentAnnouncements?.gone()
                _binding?.tvEmptyAnnouncements?.visible()
                _binding?.rvRecentDevotionals?.gone()
                _binding?.tvEmptyDevotionals?.visible()
                return@launch
            }
            
            // Fetch from network
            try {
                val statsResponse = ApiUtils.executeWithRefresh { app.apiService.getDashboardStats() }
                
                if (statsResponse.isSuccessful && statsResponse.body() != null) {
                    val stats = statsResponse.body()!!
                    // Cache the stats
                    app.database.dashboardStatsDao().insertDashboardStats(stats.toEntity())
                    updateUI(stats)
                } else {
                    requireContext().showToast("✗ Failed to load dashboard stats")
                }
                
                // Load announcements and devotionals in parallel for better performance
                val announcementsJob = lifecycleScope.launch { loadRecentAnnouncements(true) }
                val devotionalsJob = lifecycleScope.launch { loadRecentDevotionals(true) }
                
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("✗ Network error: ${e.message ?: "Unknown error"}")
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }
    
    private fun loadRecentAnnouncements(fetchFromNetwork: Boolean = true) {
        lifecycleScope.launch {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                _binding?.rvRecentAnnouncements?.gone()
                _binding?.tvEmptyAnnouncements?.visible()
                return@launch
            }
            
            // Fetch from network
            try {
                val response = ApiUtils.executeWithRefresh { app.apiService.getAnnouncements(page = 1) }
                
                if (response.isSuccessful && response.body() != null) {
                    val announcements = response.body()!!.results.take(3) // Take only first 3
                    
                    // Cache the announcements
                    app.database.announcementDao().deleteAllAnnouncements()
                    app.database.announcementDao().insertAnnouncements(announcements.map { it.toEntity() })
                    
                    if (announcements.isEmpty()) {
                        _binding?.rvRecentAnnouncements?.gone()
                        _binding?.tvEmptyAnnouncements?.visible()
                    } else {
                        _binding?.rvRecentAnnouncements?.visible()
                        _binding?.tvEmptyAnnouncements?.gone()
                        announcementAdapter.submitList(announcements)
                    }
                } else {
                    _binding?.rvRecentAnnouncements?.gone()
                    _binding?.tvEmptyAnnouncements?.visible()
                    requireContext().showToast("✗ Failed to load announcements")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _binding?.rvRecentAnnouncements?.gone()
                _binding?.tvEmptyAnnouncements?.visible()
                requireContext().showToast("✗ Network error loading announcements")
            }
        }
    }
    
    private fun loadRecentDevotionals(fetchFromNetwork: Boolean = true) {
        lifecycleScope.launch {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                binding.rvRecentDevotionals.gone()
                binding.tvEmptyDevotionals.visible()
                return@launch
            }
            
            // Fetch from network
            try {
                val response = ApiUtils.executeWithRefresh { app.apiService.getDevotionals(page = 1) }
                
                if (response.isSuccessful && response.body() != null) {
                    val devotionals = response.body()!!.results.take(3) // Take only first 3
                    
                    // Cache the devotionals
                    app.database.devotionalDao().deleteAllDevotionals()
                    app.database.devotionalDao().insertDevotionals(devotionals.map { it.toEntity() })
                    
                    if (devotionals.isEmpty()) {
                        binding.rvRecentDevotionals.gone()
                        binding.tvEmptyDevotionals.visible()
                    } else {
                        binding.rvRecentDevotionals.visible()
                        binding.tvEmptyDevotionals.gone()
                        devotionalAdapter.submitList(devotionals)
                    }
                } else {
                    binding.rvRecentDevotionals.gone()
                    binding.tvEmptyDevotionals.visible()
                    requireContext().showToast("✗ Failed to load devotionals")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.rvRecentDevotionals.gone()
                binding.tvEmptyDevotionals.visible()
                requireContext().showToast("✗ Network error loading devotionals")
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
