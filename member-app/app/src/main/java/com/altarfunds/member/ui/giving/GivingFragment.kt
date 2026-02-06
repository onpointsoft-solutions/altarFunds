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
import com.altarfunds.member.databinding.FragmentGivingBinding
import com.altarfunds.member.utils.*
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
            try {
                val response = app.apiService.getDonations()
                
                if (response.isSuccessful && response.body() != null) {
                    val donations = response.body()!!.results
                    donationAdapter.submitList(donations)
                    
                    if (donations.isEmpty()) {
                        binding.tvEmpty.visible()
                        binding.rvDonations.gone()
                    } else {
                        binding.tvEmpty.gone()
                        binding.rvDonations.visible()
                    }
                } else {
                    requireContext().showToast("Failed to load donations")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("Network error")
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
