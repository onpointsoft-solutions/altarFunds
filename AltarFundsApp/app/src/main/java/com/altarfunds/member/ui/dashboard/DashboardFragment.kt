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
    }
    
    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            // Load cached data first
            val cachedStats = app.database.dashboardStatsDao().getDashboardStats().firstOrNull()
            if (cachedStats != null) {
                updateUI(cachedStats.toModel())
            }
            
            // Check network availability
            val isOnline = NetworkUtils.isNetworkAvailable(requireContext())
            
            if (!isOnline) {
                binding.swipeRefresh.isRefreshing = false
                if (cachedStats != null) {
                    requireContext().showToast("ℹ Offline mode - Showing cached dashboard data")
                } else {
                    requireContext().showToast("✗ No internet connection and no cached data")
                }
                // Still load cached announcements and devotionals
                loadRecentAnnouncements(false)
                loadRecentDevotionals(false)
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
                    if (cachedStats == null) {
                        val errorMessage = when (statsResponse.code()) {
                            404 -> "✗ Dashboard stats not available"
                            401 -> "✗ Session expired. Please login again."
                            403 -> "✗ Access denied"
                            500 -> "✗ Server error. Please try again later."
                            else -> "✗ Failed to load dashboard: ${statsResponse.message()}"
                        }
                        requireContext().showToast(errorMessage)
                    }
                }
                
                // Load announcements and devotionals in parallel for better performance
                val announcementsJob = lifecycleScope.launch { loadRecentAnnouncements(true) }
                val devotionalsJob = lifecycleScope.launch { loadRecentDevotionals(true) }
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (cachedStats == null) {
                    requireContext().showToast("✗ Network error: ${e.message ?: "Unknown error"}")
                }
                // Load cached data as fallback
                loadRecentAnnouncements(false)
                loadRecentDevotionals(false)
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }
    
    private fun loadRecentAnnouncements(fetchFromNetwork: Boolean = true) {
        lifecycleScope.launch {
            // Load from cache first
            val cachedAnnouncements = app.database.announcementDao().getRecentAnnouncements(3).firstOrNull()
            if (!cachedAnnouncements.isNullOrEmpty()) {
                val announcements = cachedAnnouncements.map { it.toModel() }
                _binding?.rvRecentAnnouncements?.visible()
                _binding?.tvEmptyAnnouncements?.gone()
                announcementAdapter.submitList(announcements)
            }
            
            if (!fetchFromNetwork || !NetworkUtils.isNetworkAvailable(requireContext())) {
                if (cachedAnnouncements.isNullOrEmpty()) {
                    _binding?.rvRecentAnnouncements?.gone()
                    _binding?.tvEmptyAnnouncements?.visible()
                }
                return@launch
            }
            
            // Fetch from network
            try {
                val response = ApiUtils.executeWithRefresh { app.apiService.getAnnouncements(page = 1) }
                
                if (response.isSuccessful && response.body() != null) {
                    val announcements = response.body()!!.results.take(3) // Take only first 3
                    
                    // Cache the announcements
                    if (announcements.isNotEmpty()) {
                        app.database.announcementDao().deleteAllAnnouncements()
                        app.database.announcementDao().insertAnnouncements(announcements.map { it.toEntity() })
                    }
                    
                    if (announcements.isEmpty()) {
                        _binding?.rvRecentAnnouncements?.gone()
                        _binding?.tvEmptyAnnouncements?.visible()
                    } else {
                        _binding?.rvRecentAnnouncements?.visible()
                        _binding?.tvEmptyAnnouncements?.gone()
                        announcementAdapter.submitList(announcements)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Keep cached data if available
            }
        }
    }
    
    private fun loadRecentDevotionals(fetchFromNetwork: Boolean = true) {
        lifecycleScope.launch {
            // Load from cache first
            val cachedDevotionals = app.database.devotionalDao().getRecentDevotionals(3).firstOrNull()
            if (!cachedDevotionals.isNullOrEmpty()) {
                val devotionals = cachedDevotionals.map { it.toModel() }
                binding.rvRecentDevotionals.visible()
                binding.tvEmptyDevotionals.gone()
                devotionalAdapter.submitList(devotionals)
            }
            
            if (!fetchFromNetwork || !NetworkUtils.isNetworkAvailable(requireContext())) {
                if (cachedDevotionals.isNullOrEmpty()) {
                    binding.rvRecentDevotionals.gone()
                    binding.tvEmptyDevotionals.visible()
                }
                return@launch
            }
            
            // Fetch from network
            try {
                val response = ApiUtils.executeWithRefresh { app.apiService.getDevotionals(page = 1) }
                
                if (response.isSuccessful && response.body() != null) {
                    val devotionals = response.body()!!.results.take(3) // Take only first 3
                    
                    // Cache the devotionals
                    if (devotionals.isNotEmpty()) {
                        app.database.devotionalDao().deleteAllDevotionals()
                        app.database.devotionalDao().insertDevotionals(devotionals.map { it.toEntity() })
                    }
                    
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
                // Keep cached data if available
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
