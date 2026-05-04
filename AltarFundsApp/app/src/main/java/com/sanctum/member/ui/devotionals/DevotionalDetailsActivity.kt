package com.sanctum.member.ui.devotionals

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sanctum.member.MemberApp
import com.sanctum.member.adapters.CommentAdapter
import com.sanctum.member.adapters.ReactionAdapter
import com.sanctum.member.R
import com.sanctum.member.databinding.ActivityDevotionalDetailsBinding
import com.sanctum.member.utils.*
import com.google.android.material.chip.Chip
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevotionalDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDevotionalDetailsBinding
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var reactionAdapter: ReactionAdapter
    private var devotionalId: Int = -1
    private var userReactions: MutableSet<String> = mutableSetOf()
    
    private val reactionTypes = mapOf(
        R.id.chipLove to Pair("love", "❤️"),
        R.id.chipPray to Pair("pray", "🙏"),
        R.id.chipThumbsUp to Pair("thumbs_up", "👍"),
        R.id.chipFire to Pair("fire", "🔥"),
        R.id.chipCelebrate to Pair("celebrate", "🎉")
    )
    
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
        devotionalId = intent.getIntExtra("devotional_id", -1)
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
    
    private fun displayDevotional(devotional: com.sanctum.member.models.Devotional) {
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
            // Use professional background when no image
            binding.ivBanner.setImageResource(R.drawable.bg_professional_banner)
            binding.tvBannerTitle.text = devotional.title
            binding.tvBannerSubtitle.text = "By ${devotional.author}"
            binding.tvBannerTitle.visible()
            binding.tvBannerSubtitle.visible()
        }
        
        // Set content
        binding.tvTitle.text = devotional.title
        binding.tvScripture.text = devotional.scriptureReference ?: ""
        binding.tvBannerTitle.text = "By ${devotional.author}"
        binding.tvDate.text = devotional.date.formatDate()
        binding.tvContent.text = devotional.content
        
        // Set reaction counts from API response
        binding.tvLikeCount.text = (devotional.likeCount ?: 0).toString()
        binding.tvCommentCount.text = (devotional.commentCount ?: 0).toString()
        
        // Show total reactions count if available
        val reactionsCount = devotional.reactionsCount ?: 0
        if (reactionsCount > 0) {
            binding.tvReactionCount.text = "$reactionsCount reactions"
            binding.tvReactionCount.visible()
        } else {
            binding.tvReactionCount.gone()
        }
        
        // Set bookmark state
        if (devotional.isBookmarked) {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            binding.btnBookmark.setColorFilter(ContextCompat.getColor(this, R.color.primary))
        } else {
            binding.btnBookmark.setImageResource(R.drawable.ic_bookmark_outline)
            binding.btnBookmark.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary))
        }
        
        // Set like state
        if (devotional.isLiked) {
            binding.btnLike.setImageResource(R.drawable.ic_heart_filled)
            binding.btnLike.setColorFilter(ContextCompat.getColor(this, R.color.primary))
        } else {
            binding.btnLike.setImageResource(R.drawable.ic_heart_outline)
            binding.btnLike.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary))
        }
        
        // Show user's current reaction if available
        val userReaction = devotional.userReaction
        if (!userReaction.isNullOrEmpty()) {
            // Update user reactions set
            userReactions.clear()
            userReactions.add(userReaction)
            
            // Show toast indicating current reaction
            val emoji = getEmojiForReaction(userReaction)
            showToast("You reacted with $emoji")
        }
        
        setupClickListeners(devotional)
        
        // Load all related data
        loadAllRelatedData()
    }
    
    private fun loadAllRelatedData() {
        // Load reactions and comments in parallel for better performance
        lifecycleScope.launch {
            try {
                // Load reactions and comments concurrently
                val reactionsDeferred = async { loadReactions() }
                val commentsDeferred = async { loadComments() }
                
                // Wait for both to complete
                reactionsDeferred.await()
                commentsDeferred.await()
                
                // Hide loading indicators
                hideLoadingIndicators()
                
            } catch (e: Exception) {
                e.printStackTrace()
                hideLoadingIndicators()
                showToast("Some data failed to load")
            }
        }
    }
    
    private fun hideLoadingIndicators() {
        // Hide any loading states if they exist
        // This can be expanded based on UI loading indicators
    }
    
    private fun getEmojiForReaction(reactionType: String): String {
        return when (reactionType) {
            "love" -> "â¤ï¸"
            "pray" -> "ðŸ"
            "thumbs_up" -> "ðŸ‘"
            "fire" -> "ðŸ”¥"
            "celebrate" -> "ðŸŽ‰"
            "like" -> "â¤ï¸"
            else -> "â¤ï¸"
        }
    }
    
    private fun setupClickListeners(devotional: com.sanctum.member.models.Devotional) {
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
            // Focus on comment input
            binding.etComment.requestFocus()
            // Show keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
        }
        
        // Send comment button
        binding.btnSendComment.setOnClickListener {
            postComment(devotional.id ?: -1)
        }
        
        // Bookmark button
        binding.btnBookmark.setOnClickListener {
            toggleBookmark(devotional.id ?: -1)
        }
        
        // Setup emoji reaction chips
        setupEmojiReactions()
    }
    
    private fun setupEmojiReactions() {
        reactionTypes.forEach { (chipId, reactionPair) ->
            val reactionType = reactionPair.first
            val emoji = reactionPair.second
            val chip = findViewById<Chip>(chipId)
            chip?.setOnClickListener {
                toggleReaction(reactionType, emoji)
            }
        }
    }
    
    private fun toggleReaction(reactionType: String, emoji: String) {
        if (devotionalId == -1) return
        
        val isAdding = !userReactions.contains(reactionType)
        
        lifecycleScope.launch {
            try {
                val request = mapOf("reaction_type" to reactionType)
                val response = app.apiService.likeDevotional(devotionalId, request)
                
                if (response.isSuccessful && response.body() != null) {
                    val likeResponse = response.body()!!
                    
                    if (isAdding) {
                        userReactions.add(reactionType)
                        showToast("Reacted with $emoji")
                    } else {
                        userReactions.remove(reactionType)
                        showToast("Removed $emoji reaction")
                    }
                    
                    // Update chip visual state
                    updateReactionChipState(reactionType, isAdding)
                    
                    // Reload reactions to show the update
                    loadReactions()
                    
                    // Update like count
                    binding.tvLikeCount.text = likeResponse.likeCount.toString()
                } else {
                    showToast("✗ Failed to update reaction")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("✗ Network error: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    private fun updateReactionChipState(reactionType: String, isSelected: Boolean) {
        val chipEntry = reactionTypes.entries.find { it.value.first == reactionType }
        chipEntry?.let { entry ->
            val chip = findViewById<Chip>(entry.key)
            chip?.apply {
                if (isSelected) {
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this@DevotionalDetailsActivity, R.color.chip_selected)
                    )
                } else {
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this@DevotionalDetailsActivity, R.color.chip_unselected)
                    )
                }
            }
        }
    }
    
    private fun updateReactionCounts(devotional: com.sanctum.member.models.Devotional) {
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
                val response = app.apiService.getDevotionalComments(devotionalId)
                
                if (response.isSuccessful && response.body() != null) {
                    val comments = response.body()!!
                    commentAdapter.submitList(comments)
                    
                    // Update UI based on comments availability
                    if (comments.isNotEmpty()) {
                        binding.tvNoComments.gone()
                        binding.rvComments.visible()
                    } else {
                        binding.tvNoComments.visible()
                        binding.rvComments.gone()
                    }
                    
                    // Update comment count
                    binding.tvCommentCount.text = comments.size.toString()
                } else {
                    // Handle server errors
                    binding.tvNoComments.visible()
                    binding.rvComments.gone()
                    binding.tvCommentCount.text = "0"
                    
                    val errorMessage = when (response.code()) {
                        401 -> "Session expired. Please login again."
                        403 -> "Access denied."
                        404 -> "Comments not available."
                        500 -> "Server error loading comments."
                        else -> "Failed to load comments."
                    }
                    showToast("× $errorMessage")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvNoComments.visible()
                binding.rvComments.gone()
                binding.tvCommentCount.text = "0"
                showToast("× Network error loading comments")
            }
        }
    }
    
    private fun loadReactions() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionalReactions(devotionalId)
                
                if (response.isSuccessful && response.body() != null) {
                    val reactions = response.body()!!
                    reactionAdapter.submitList(reactions)
                    
                    // Check which reactions belong to current user in background
                    withContext(kotlinx.coroutines.Dispatchers.Default) {
                        val userEmail = app.tokenManager.getUserEmail()
                        userReactions.clear()
                        
                        reactions.forEach { reaction ->
                            if (reaction.userName == userEmail) {
                                userReactions.add(reaction.reactionType)
                            }
                        }
                    }
                    
                    // Update UI on main thread
                    reactions.forEach { reaction ->
                        if (userReactions.contains(reaction.reactionType)) {
                            updateReactionChipState(reaction.reactionType, true)
                        }
                    }
                } else {
                    // Clear user reactions if none found
                    userReactions.clear()
                    // Reset all chips to unselected state
                    reactionTypes.values.forEach { (reactionType, _) ->
                        updateReactionChipState(reactionType, false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Clear user reactions on error
                userReactions.clear()
                reactionTypes.values.forEach { (reactionType, _) ->
                    updateReactionChipState(reactionType, false)
                }
            }
        }
    }
    
    private fun shareDevotional(devotional: com.sanctum.member.models.Devotional) {
        val shareText = "${devotional.title}\n\n${devotional.scriptureReference ?: ""}\n\nBy ${devotional.author}\n\nRead more in AltarFunds App"
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            putExtra(android.content.Intent.EXTRA_SUBJECT, devotional.title)
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Devotional"))
    }
    
    private fun postComment(devotionalId: Int) {
        if (devotionalId == -1) return
        
        val commentText = binding.etComment.text.toString().trim()
        if (commentText.isEmpty()) {
            showToast("Please enter a comment")
            return
        }
        
        // Disable send button to prevent duplicate submissions
        binding.btnSendComment.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val request = mapOf("content" to commentText, "devotional" to devotionalId.toString())
                val response = app.apiService.postComment(devotionalId, request)
                
                if (response.isSuccessful && response.body() != null) {
                    val newComment = response.body()!!
                    
                    // Clear comment input immediately
                    binding.etComment.text.clear()
                    
                    // Add new comment to adapter directly for immediate feedback
                    val currentComments = commentAdapter.currentList.toMutableList()
                    currentComments.add(0, newComment) // Add at top
                    commentAdapter.submitList(currentComments)
                    
                    // Update UI to show comments
                    binding.tvNoComments.gone()
                    binding.rvComments.visible()
                    
                    // Update comment count
                    val currentCount = binding.tvCommentCount.text.toString().toIntOrNull() ?: 0
                    binding.tvCommentCount.text = (currentCount + 1).toString()
                    
                    showToast("Comment posted successfully")
                    
                    // Also refresh from server to ensure consistency
                    kotlinx.coroutines.delay(1000) // Wait a moment then refresh
                    loadComments()
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Session expired. Please login again."
                        403 -> "Access denied."
                        400 -> "Invalid comment content."
                        404 -> "Devotional not found."
                        500 -> "Server error posting comment."
                        else -> "Failed to post comment."
                    }
                    showToast("× $errorMessage")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("× Network error: ${e.message ?: "Unable to post comment"}")
            } finally {
                // Re-enable send button
                binding.btnSendComment.isEnabled = true
            }
        }
    }
}
