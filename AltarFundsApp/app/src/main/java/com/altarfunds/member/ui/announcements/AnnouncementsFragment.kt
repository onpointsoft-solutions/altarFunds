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
        _binding?.rvAnnouncements?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = announcementAdapter
        }
    }
    
    private fun setupListeners() {
        _binding?.swipeRefresh?.setOnRefreshListener {
            loadAnnouncements()
        }
    }
    
    private fun loadAnnouncements() {
        _binding?.swipeRefresh?.isRefreshing = true
        
        lifecycleScope.launch {
            // Clear cached data first to force fresh load
            app.database.announcementDao().deleteAllAnnouncements()
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                _binding?.swipeRefresh?.isRefreshing = false
                requireContext().showToast("✗ No internet connection")
                _binding?.let { binding ->
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
                        app.database.announcementDao().insertAnnouncements(announcements.map { it.toEntity() })
                    }
                    
                    announcementAdapter.submitList(announcements)
                    
                    _binding?.let { binding ->
                        if (announcements.isEmpty()) {
                            binding.tvEmpty.visible()
                            binding.rvAnnouncements.gone()
                        } else {
                            binding.tvEmpty.gone()
                            binding.rvAnnouncements.visible()
                        }
                    }
                } else {
                    requireContext().showToast("✗ Failed to load announcements")
                    _binding?.let { binding ->
                        binding.tvEmpty.visible()
                        binding.rvAnnouncements.gone()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("✗ Network error: ${e.message ?: "Unknown error"}")
                _binding?.let { binding ->
                    binding.tvEmpty.visible()
                    binding.rvAnnouncements.gone()
                }
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
