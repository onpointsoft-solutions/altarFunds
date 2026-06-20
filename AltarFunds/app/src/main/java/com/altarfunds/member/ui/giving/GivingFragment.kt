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

    // Tracks whether we've loaded data for this instance — prevents double load
    // from onViewCreated + onResume firing back-to-back
    private var initialLoadDone = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGivingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        // First load
        loadDonations()
        initialLoadDone = true
    }

    override fun onResume() {
        super.onResume()
        // Refresh after returning from GivingActivity (new donation may have been made)
        // but skip the very first resume that fires immediately after onViewCreated
        if (initialLoadDone) {
            loadDonations()
        }
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
        if (_binding == null) return          // fragment view already destroyed
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = app.apiService.getDonations()

                if (_binding == null) return@launch   // view destroyed while awaiting

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
                    val msg = when (response.code()) {
                        401  -> "Session expired. Please log in again."
                        403  -> "Access denied."
                        500  -> "Server error. Please try again later."
                        else -> "Failed to load donations (${response.code()})."
                    }
                    requireContext().showToast(msg)
                    // Show empty state so the user isn't looking at a blank screen
                    binding.tvEmpty.visible()
                    binding.rvDonations.gone()
                }

            } catch (e: java.net.UnknownHostException) {
                if (_binding != null) {
                    requireContext().showToast("✗ No internet connection.")
                    binding.tvEmpty.visible()
                    binding.rvDonations.gone()
                }
            } catch (e: java.net.SocketTimeoutException) {
                if (_binding != null) {
                    requireContext().showToast("✗ Connection timed out.")
                    binding.tvEmpty.visible()
                    binding.rvDonations.gone()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_binding != null) {
                    requireContext().showToast("✗ ${e.message ?: "Network error"}")
                    binding.tvEmpty.visible()
                    binding.rvDonations.gone()
                }
            } finally {
                if (_binding != null) binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
