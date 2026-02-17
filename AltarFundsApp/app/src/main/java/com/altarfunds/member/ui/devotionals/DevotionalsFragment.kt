package com.altarfunds.member.ui.devotionals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.adapters.DevotionalAdapter
import com.altarfunds.member.data.mappers.toEntity
import com.altarfunds.member.data.mappers.toModel
import com.altarfunds.member.databinding.FragmentDevotionalsBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DevotionalsFragment : Fragment() {
    
    private var _binding: FragmentDevotionalsBinding? = null
    private val binding get() = _binding!!
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var devotionalAdapter: DevotionalAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevotionalsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        loadDevotionals()
    }
    
    private fun setupRecyclerView() {
        devotionalAdapter = DevotionalAdapter()
        binding.rvDevotionals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = devotionalAdapter
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadDevotionals()
        }
    }
    
    private fun loadDevotionals() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            // First, try to load from cache
            val cachedDevotionals = app.database.devotionalDao().getAllDevotionals().firstOrNull()
            if (!cachedDevotionals.isNullOrEmpty()) {
                val devotionals = cachedDevotionals.map { it.toModel() }
                devotionalAdapter.submitList(devotionals)
                binding.tvEmpty.gone()
                binding.rvDevotionals.visible()
            }
            
            // Check network availability
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                binding.swipeRefresh.isRefreshing = false
                if (!cachedDevotionals.isNullOrEmpty()) {
                    requireContext().showToast("ℹ Offline mode - Showing cached devotionals")
                } else {
                    requireContext().showToast("✗ No internet connection and no cached devotionals")
                    binding.tvEmpty.visible()
                    binding.rvDevotionals.gone()
                }
                return@launch
            }
            
            // Fetch from network
            try {
                val response = app.apiService.getDevotionals()
                
                if (response.isSuccessful && response.body() != null) {
                    val devotionals = response.body()!!.results
                    
                    // Cache the devotionals
                    if (devotionals.isNotEmpty()) {
                        app.database.devotionalDao().deleteAllDevotionals()
                        app.database.devotionalDao().insertDevotionals(devotionals.map { it.toEntity() })
                    }
                    
                    devotionalAdapter.submitList(devotionals)
                    
                    if (devotionals.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvDevotionals.gone()
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvDevotionals.visible()
                    }
                } else {
                    if (cachedDevotionals.isNullOrEmpty()) {
                        requireContext().showToast("✗ Failed to load devotionals")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cachedDevotionals.isNullOrEmpty()) {
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
