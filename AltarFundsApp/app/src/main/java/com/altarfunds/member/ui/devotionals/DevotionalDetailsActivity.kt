package com.altarfunds.member.ui.devotionals

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.altarfunds.member.MemberApp
import com.altarfunds.member.adapters.CommentAdapter
import com.altarfunds.member.adapters.ReactionAdapter
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ActivityDevotionalDetailsBinding
import com.altarfunds.member.utils.*
import kotlinx.coroutines.launch

class DevotionalDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDevotionalDetailsBinding
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var reactionAdapter: ReactionAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevotionalDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerViews()
        loadDevotionalDetails()
        loadComments()
        loadReactions()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Devotional Details"
        }
    }
    
    private fun setupRecyclerViews() {
        // Comments RecyclerView
        commentAdapter = CommentAdapter()
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@DevotionalDetailsActivity)
            adapter = commentAdapter
        }
        
        // Reactions RecyclerView
        reactionAdapter = ReactionAdapter()
        binding.rvReactions.apply {
            layoutManager = LinearLayoutManager(this@DevotionalDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = reactionAdapter
        }
    }
    
    private fun loadDevotionalDetails() {
        val devotionalId = intent.getIntExtra("devotional_id", -1)
        if (devotionalId == -1) {
            finish()
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionalDetails(devotionalId)
                
                if (response.isSuccessful && response.body() != null) {
                    val devotional = response.body()!!
                    displayDevotional(devotional)
                } else {
                    showToast("✗ Failed to load devotional")
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ Network error: ${e.message ?: "Unknown error"}")
                finish()
            }
        }
    }
    
    private fun displayDevotional(devotional: com.altarfunds.member.models.Devotional) {
        // Enhanced banner with gradient background
        if (!devotional.bannerImage.isNullOrEmpty()) {
            val bannerColors = intArrayOf(
                ContextCompat.getColor(this, R.color.primary),
                ContextCompat.getColor(this, R.color.secondary),
                ContextCompat.getColor(this, R.color.accent)
            )
            val gradient = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                bannerColors
            )
            gradient.cornerRadius = 24f
            binding.ivBanner.background = gradient
            
            // Add title overlay on banner
            binding.tvBannerTitle.text = devotional.title
            binding.tvBannerSubtitle.text = "By ${devotional.author}"
            binding.tvBannerTitle.visible()
            binding.tvBannerSubtitle.visible()
        } else {
            binding.ivBanner.setImageResource(R.drawable.ic_devotional_placeholder)
            binding.tvBannerTitle.gone()
            binding.tvBannerSubtitle.gone()
        }
        
        // Content
        binding.tvTitle.text = devotional.title
        binding.tvScripture.text = devotional.scriptureReference ?: ""
        binding.tvContent.text = devotional.content
        
        // Metadata
        binding.tvDate.text = devotional.date.formatDate()
        binding.tvAuthor.text = "By ${devotional.author}"
        
        // Reactions
        updateReactionCounts(devotional)
        
        // Load actual reactions and comments
        loadReactions()
        loadComments()
        
        // Share button
        binding.btnShare.setOnClickListener {
            shareDevotional(devotional)
        }
        
        // Like button
        binding.btnLike.setOnClickListener {
            toggleLike(devotional.id ?: -1)
        }
        
        // Comment button
        binding.btnComment.setOnClickListener {
            // Open comment dialog or navigate to comment section
        }
        
        // Bookmark button
        binding.btnBookmark.setOnClickListener {
            toggleBookmark(devotional.id ?: -1)
        }
    }
    
    private fun updateReactionCounts(devotional: com.altarfunds.member.models.Devotional) {
        binding.tvLikeCount.text = (devotional.likeCount ?: 0).toString()
        binding.tvCommentCount.text = (devotional.commentCount ?: 0).toString()
        
        // Update reaction buttons state
        binding.btnLike.isSelected = devotional.isLiked ?: false
        binding.btnBookmark.isSelected = devotional.isBookmarked ?: false
        
        val likeIcon = if (devotional.isLiked == true) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        val bookmarkIcon = if (devotional.isBookmarked == true) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        
        binding.btnLike.setImageResource(likeIcon)
        binding.btnBookmark.setImageResource(bookmarkIcon)
    }
    
    private fun toggleLike(devotionalId: Int) {
        if (devotionalId == -1) return
        
        lifecycleScope.launch {
            try {
                val request = mapOf("reaction_type" to "like")
                val response = app.apiService.likeDevotional(devotionalId, request)
                if (response.isSuccessful && response.body() != null) {
                    val likeResponse = response.body()!!
                    binding.tvLikeCount.text = likeResponse.likeCount.toString()
                    binding.btnLike.isSelected = likeResponse.isLiked
                    
                    val likeIcon = if (likeResponse.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                    binding.btnLike.setImageResource(likeIcon)
                    
                    showToast(likeResponse.message)
                } else {
                    showToast("✗ Failed to like devotional")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ Network error: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    private fun toggleBookmark(devotionalId: Int) {
        if (devotionalId == -1) return
        
        lifecycleScope.launch {
            try {
                val response = app.apiService.bookmarkDevotional(devotionalId)
                if (response.isSuccessful && response.body() != null) {
                    val bookmarkResponse = response.body()!!
                    binding.btnBookmark.isSelected = bookmarkResponse.isBookmarked
                    
                    val bookmarkIcon = if (bookmarkResponse.isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
                    binding.btnBookmark.setImageResource(bookmarkIcon)
                    
                    showToast(bookmarkResponse.message)
                } else {
                    showToast("✗ Failed to bookmark devotional")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ Network error: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    private fun loadComments() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionalComments(intent.getIntExtra("devotional_id", -1))
                
                if (response.isSuccessful && response.body() != null) {
                    val comments = response.body()!!
                    commentAdapter.submitList(comments)
                    binding.tvNoComments.gone()
                    binding.rvComments.visible()
                } else {
                    binding.tvNoComments.visible()
                    binding.rvComments.gone()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun loadReactions() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionalReactions(intent.getIntExtra("devotional_id", -1))
                
                if (response.isSuccessful && response.body() != null) {
                    val reactions = response.body()!!
                    reactionAdapter.submitList(reactions)
                } else {
                    // Show empty state or mock reactions
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun shareDevotional(devotional: com.altarfunds.member.models.Devotional) {
        val shareText = "${devotional.title}\n\n${devotional.scriptureReference ?: ""}\n\nBy ${devotional.author}\n\nRead more in AltarFunds App"
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            putExtra(android.content.Intent.EXTRA_SUBJECT, devotional.title)
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Devotional"))
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
