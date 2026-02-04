package com.altarfunds.mobile.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.mobile.databinding.FragmentDashboardBinding
import com.altarfunds.mobile.data.api.ApiClient
import com.altarfunds.mobile.data.models.Devotional
import com.altarfunds.mobile.data.models.Notice
import com.altarfunds.mobile.ui.adapters.DevotionalAdapter
import com.altarfunds.mobile.ui.adapters.NoticeAdapter
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var devotionalAdapter: DevotionalAdapter
    private lateinit var noticeAdapter: NoticeAdapter
    private val apiClient = ApiClient()

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
        loadData()
        setupRefreshListener()
    }

    private fun setupRecyclerViews() {
        // Setup Devotionals RecyclerView
        devotionalAdapter = DevotionalAdapter(
            onDevotionalClick = { devotional ->
                // Handle devotional click - show detail
                showDevotionalDetail(devotional)
            },
            onReactionClick = { devotional, reactionType ->
                // Handle reaction
                reactToDevotional(devotional.id, reactionType)
            },
            onCommentClick = { devotional ->
                // Handle comment
                showCommentDialog(devotional)
            }
        )
        
        binding.rvDevotionals.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = devotionalAdapter
            setHasFixedSize(true)
        }

        // Setup Notice Board RecyclerView
        noticeAdapter = NoticeAdapter(
            onNoticeClick = { notice ->
                // Handle notice click - show detail
                showNoticeDetail(notice)
            }
        )
        
        binding.rvNotices.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noticeAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                // Load devotionals
                val devotionals = apiClient.getDevotionals()
                devotionalAdapter.submitList(devotionals.take(5)) // Show latest 5
                
                // Load notices
                val notices = apiClient.getNotices()
                noticeAdapter.submitList(notices.take(5)) // Show latest 5
                
                binding.swipeRefresh.isRefreshing = false
                
                // Show/hide empty states
                binding.tvNoDevotionals.visibility = 
                    if (devotionals.isEmpty()) View.VISIBLE else View.GONE
                binding.tvNoNotices.visibility = 
                    if (notices.isEmpty()) View.VISIBLE else View.GONE
                    
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                showError("Failed to load data: ${e.message}")
            }
        }
    }

    private fun setupRefreshListener() {
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun showDevotionalDetail(devotional: Devotional) {
        // Navigate to devotional detail screen
        // TODO: Implement navigation
    }

    private fun reactToDevotional(devotionalId: Int, reactionType: String) {
        lifecycleScope.launch {
            try {
                apiClient.reactToDevotional(devotionalId, reactionType)
                loadData() // Refresh to show updated reactions
            } catch (e: Exception) {
                showError("Failed to react: ${e.message}")
            }
        }
    }

    private fun showCommentDialog(devotional: Devotional) {
        // Show dialog to add comment
        // TODO: Implement comment dialog
    }

    private fun showNoticeDetail(notice: Notice) {
        // Navigate to notice detail screen
        // TODO: Implement navigation
    }

    private fun showError(message: String) {
        // Show error message using Snackbar or Toast
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
