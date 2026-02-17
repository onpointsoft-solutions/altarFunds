package com.altarfunds.member.ui.announcements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.adapters.AnnouncementAdapter
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.data.mappers.toModel
import com.altarfunds.member.databinding.FragmentAnnouncementsBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AnnouncementsFragment : Fragment() {
    
    private var _binding: FragmentAnnouncementsBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var announcementAdapter: AnnouncementAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnnouncementsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        loadAnnouncements()
    }
    
    private fun setupRecyclerView() {
        announcementAdapter = AnnouncementAdapter()
        binding.rvAnnouncements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = announcementAdapter
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadAnnouncements()
        }
    }
    
    private fun loadAnnouncements() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            // First, try to load from cache
            val cachedAnnouncements = app.database.announcementDao().getAllAnnouncements().firstOrNull()
            if (!cachedAnnouncements.isNullOrEmpty()) {
                val announcements = cachedAnnouncements.map { it.toModel() }
                announcementAdapter.submitList(announcements)
                binding.tvEmpty.gone()
                binding.rvAnnouncements.visible()
            }
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                binding.swipeRefresh.isRefreshing = false
                if (!cachedAnnouncements.isNullOrEmpty()) {
                    requireContext().showToast("ℹ Offline mode - Showing cached announcements")
                } else {
                    requireContext().showToast("✗ No internet connection and no cached announcements")
                    binding.tvEmpty.visible()
                    binding.rvAnnouncements.gone()
                }
                return@launch
            }
            
            // Fetch from network
            try {
                val response = app.apiService.getAnnouncements()
                
                if (response.isSuccessful && response.body() != null) {
                    val announcements = response.body()!!.results
                    
                    // Cache the announcements
                    if (announcements.isNotEmpty()) {
                        app.database.announcementDao().deleteAllAnnouncements()
                        app.database.announcementDao().insertAnnouncements(announcements.map { it.toEntity() })
                    }
                    
                    announcementAdapter.submitList(announcements)
                    
                    if (announcements.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvAnnouncements.gone()
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvAnnouncements.visible()
                    }
                } else {
                    if (cachedAnnouncements.isNullOrEmpty()) {
                        requireContext().showToast("✗ Failed to load announcements")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cachedAnnouncements.isNullOrEmpty()) {
                    requireContext().showToast("✗ Network error: ${e.message ?: "Unknown error"}")
                }
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
