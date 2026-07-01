package com.sanctum.member.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sanctum.member.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.sanctum.member.MemberApp
import com.sanctum.member.adapters.AnnouncementAdapter
import com.sanctum.member.adapters.DevotionalAdapter
import com.sanctum.member.data.mappers.toEntity
import com.sanctum.member.data.mappers.toModel
import com.sanctum.member.databinding.FragmentDashboardBinding
import com.sanctum.member.ui.giving.GivingActivity
import com.sanctum.member.utils.*
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
        // When user reacts inside the card, refresh the devotionals list so
        // updated counts are visible immediately.
        devotionalAdapter.onDevotionalUpdated = { loadRecentDevotionals() }
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
                com.sanctum.member.R.id.bottomNav
            )?.selectedItemId = com.sanctum.member.R.id.nav_announcements
        }
        
        binding.btnViewAllDevotionals.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                com.sanctum.member.R.id.bottomNav
            )?.selectedItemId = com.sanctum.member.R.id.nav_devotionals
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
            showCachedDashboardData()
            
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
            if (!isAdded || context == null) return@launch
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
                    if (isAdded && context != null) {
                        requireContext().showToast("✗ Failed to load announcements")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _binding?.rvRecentAnnouncements?.gone()
                _binding?.tvEmptyAnnouncements?.visible()
                if (isAdded && context != null) {
                    requireContext().showToast("✗ Network error loading announcements")
                }
            }
        }
    }
    
    private fun loadRecentDevotionals(fetchFromNetwork: Boolean = true) {
        lifecycleScope.launch {
            if (!isAdded || context == null) return@launch
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                _binding?.rvRecentDevotionals?.gone()
                _binding?.tvEmptyDevotionals?.visible()
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
                        _binding?.rvRecentDevotionals?.gone()
                        _binding?.tvEmptyDevotionals?.visible()
                    } else {
                        _binding?.rvRecentDevotionals?.visible()
                        _binding?.tvEmptyDevotionals?.gone()
                        devotionalAdapter.submitList(devotionals)
                    }
                } else {
                    _binding?.rvRecentDevotionals?.gone()
                    _binding?.tvEmptyDevotionals?.visible()
                    if (isAdded && context != null) {
                        requireContext().showToast("✗ Failed to load devotionals")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _binding?.rvRecentDevotionals?.gone()
                _binding?.tvEmptyDevotionals?.visible()
                if (isAdded && context != null) {
                    requireContext().showToast("✗ Network error loading devotionals")
                }
            }
        }
    }
    
    private fun updateUI(stats: com.sanctum.member.models.DashboardStats) {
        _binding?.tvTotalGiven?.text = stats.totalDonations.formatCurrency()
        _binding?.tvDonationCount?.text = stats.donationCount.toString()
        _binding?.tvAnnouncementsCount?.text = stats.announcementsCount.toString()
        _binding?.tvDevotionalsCount?.text = stats.devotionalsCount.toString()
    }

    private suspend fun showCachedDashboardData() {
        // Load cached stats
        val cachedStats = app.database.dashboardStatsDao().getDashboardStats().firstOrNull()
        cachedStats?.let { updateUI(it.toModel()) }
        
        // Load cached announcements
        val cachedAnnouncements = app.database.announcementDao().getAllAnnouncements().firstOrNull()
        if (!cachedAnnouncements.isNullOrEmpty()) {
            val models = cachedAnnouncements.take(3).map { it.toModel() }
            _binding?.rvRecentAnnouncements?.visible()
            _binding?.tvEmptyAnnouncements?.gone()
            announcementAdapter.submitList(models)
        }
        
        // Load cached devotionals
        val cachedDevotionals = app.database.devotionalDao().getAllDevotionals().firstOrNull()
        if (!cachedDevotionals.isNullOrEmpty()) {
            val models = cachedDevotionals.take(3).map { it.toModel() }
            _binding?.rvRecentDevotionals?.visible()
            _binding?.tvEmptyDevotionals?.gone()
            devotionalAdapter.submitList(models)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh devotionals when returning from DevotionalDetailsActivity
        // so any new reactions/comments are reflected in the counts.
        if (_binding != null) loadRecentDevotionals()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
