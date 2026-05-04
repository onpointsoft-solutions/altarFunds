package com.sanctum.member.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sanctum.member.R
import com.sanctum.member.models.Devotional
import com.sanctum.member.utils.formatDate
import com.sanctum.member.databinding.ItemDevotionalBinding
import com.sanctum.member.ui.devotionals.DevotionalDetailsActivity
import com.sanctum.member.utils.gone
import com.sanctum.member.utils.visible
import com.bumptech.glide.Glide
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.sanctum.member.MemberApp
import android.content.Context

class DevotionalAdapter : ListAdapter<Devotional, DevotionalAdapter.ViewHolder>(DiffCallback()) {
    
    private var onDevotionalUpdated: (() -> Unit)? = null
    
    fun setOnDevotionalUpdatedListener(listener: () -> Unit) {
        onDevotionalUpdated = listener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDevotionalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemDevotionalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        private var isLiked = false
        private var isBookmarked = false
        private var likeCount = 0
        private var commentCount = 0
        
        fun bind(devotional: Devotional) {
            // Set basic content
            binding.tvTitle.text = devotional.title
            binding.tvScripture.text = devotional.scriptureReference ?: ""
            binding.tvAuthor.text = "By ${devotional.author}"
            binding.tvDate.text = devotional.date.formatDate()
            binding.tvPreview.text = devotional.content.take(120) + if (devotional.content.length > 120) "..." else ""
            
            // Load banner image with better fallback
            if (!devotional.bannerImage.isNullOrEmpty()) {
                // Load banner image if available
                Glide.with(binding.root.context)
                    .load(devotional.bannerImage)
                    .placeholder(R.drawable.bg_professional_banner)
                    .error(R.drawable.bg_professional_banner)
                    .into(binding.ivBanner)
                
                // Set banner title
                binding.tvBannerTitle.text = devotional.title.take(30) + if (devotional.title.length > 30) "..." else ""
                binding.tvBannerTitle.visible()
            } else {
                // Use professional background when no image
                binding.ivBanner.setImageResource(R.drawable.bg_professional_banner)
                binding.tvBannerTitle.text = devotional.title.take(30) + if (devotional.title.length > 30) "..." else ""
                binding.tvBannerTitle.visible()
            }
            
            // Set real reaction counts
            likeCount = devotional.likeCount ?: 0
            commentCount = devotional.commentCount ?: 0
            val reactionsCount = devotional.reactionsCount ?: 0
            val userReaction = devotional.userReaction
            
            // Update reaction display
            updateReactionCounts()
            
            // Show reactions if there are any
            if (reactionsCount > 0) {
                binding.llReactions.visible()
                binding.tvReactions.text = reactionsCount.toString()
                
                // Show user's reaction emoji if they reacted
                if (!userReaction.isNullOrEmpty()) {
                    val emoji = getEmojiForReaction(userReaction)
                    binding.tvReactionEmoji.text = emoji
                    binding.tvReactionEmoji.visible()
                } else {
                    binding.tvReactionEmoji.text = ""
                    binding.tvReactionEmoji.gone()
                }
            } else {
                binding.llReactions.gone()
            }
            
            // Set click listeners
            setupClickListeners(devotional)
        }
        
        private fun setupClickListeners(devotional: Devotional) {
            // Click on entire item to open details
            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, DevotionalDetailsActivity::class.java)
                intent.putExtra("devotional_id", devotional.id)
                context.startActivity(intent)
            }
            
            // Like button
            binding.btnLike.setOnClickListener {
                val devotionalId = devotional.id ?: return@setOnClickListener
                val context = binding.root.context
                
                // Toggle like state
                isLiked = !isLiked
                
                // Save reaction to API
                saveReactionToApi(context, devotionalId, if (isLiked) "love" else "love", isLiked)
                
                // Update UI immediately for feedback
                if (isLiked) {
                    likeCount++
                    binding.ivLike.setImageResource(com.sanctum.member.R.drawable.ic_heart_filled)
                    binding.ivLike.setColorFilter(ContextCompat.getColor(context, com.sanctum.member.R.color.primary))
                } else {
                    likeCount--
                    binding.ivLike.setImageResource(com.sanctum.member.R.drawable.ic_heart_outline)
                    binding.ivLike.setColorFilter(ContextCompat.getColor(context, com.sanctum.member.R.color.text_secondary))
                }
                updateReactionCounts()
            }
            
            // Comment button
            binding.btnComment.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, DevotionalDetailsActivity::class.java)
                intent.putExtra("devotional_id", devotional.id)
                intent.putExtra("open_comments", true)
                context.startActivity(intent)
            }
            
            // Share button
            binding.btnShare.setOnClickListener {
                val context = binding.root.context
                val shareText = "${devotional.title}\n\n${devotional.scriptureReference ?: ""}\n\nBy ${devotional.author}"
                
                // Show share options
                val shareOptions = arrayOf("Share with App", "Share as Text", "Copy Link")
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Share Devotional")
                    .setItems(shareOptions) { dialog, which ->
                        when (which) {
                            0 -> shareWithApp(context, devotional)
                            1 -> shareAsText(context, shareText)
                            2 -> copyLink(context, devotional)
                        }
                    }
                    .show()
            }
            
            // Bookmark button
            binding.btnBookmark.setOnClickListener {
                isBookmarked = !isBookmarked
                if (isBookmarked) {
                    binding.ivBookmark.setImageResource(com.sanctum.member.R.drawable.ic_bookmark_filled)
                    binding.ivBookmark.setColorFilter(ContextCompat.getColor(binding.root.context, com.sanctum.member.R.color.primary))
                } else {
                    binding.ivBookmark.setImageResource(com.sanctum.member.R.drawable.ic_bookmark_outline)
                    binding.ivBookmark.setColorFilter(ContextCompat.getColor(binding.root.context, com.sanctum.member.R.color.text_secondary))
                }
            }
        }
        
        private fun updateReactionCounts() {
            binding.tvLikeCount.text = likeCount.toString()
            binding.tvCommentCount.text = commentCount.toString()
        }
        
        private fun getEmojiForReaction(reactionType: String): String {
            return when (reactionType) {
                "love" -> "â¤ï¸"
                "pray" -> "ð"
                "thumbs_up" -> "ð"
                "fire" -> "ð"
                "celebrate" -> "ð"
                "like" -> "â¤ï¸"
                else -> "â¤ï¸"
            }
        }
        
        private fun saveReactionToApi(context: Context, devotionalId: Int, reactionType: String, isAdding: Boolean) {
            val app = MemberApp.getInstance()
            
            // Use a coroutine scope to call the API
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    val response = if (isAdding) {
                        app.apiService.reactToDevotional(devotionalId, mapOf("reaction_type" to reactionType))
                    } else {
                        app.apiService.removeDevotionalReaction(devotionalId, reactionType)
                    }
                    
                    if (response.isSuccessful) {
                        // Refresh the list to show updated reaction counts
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onDevotionalUpdated?.invoke()
                        }
                    } else {
                        // Show error on main thread
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (context is android.app.Activity) {
                                context.runOnUiThread {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to save reaction",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Show error on main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (context is android.app.Activity) {
                            context.runOnUiThread {
                                android.widget.Toast.makeText(
                                    context,
                                    "Network error",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun shareWithApp(context: android.content.Context, devotional: Devotional) {
        try {
            // Generate deep link for app
            val deepLink = "altarfunds://devotional/${devotional.id}"
            val appStoreUrl = "https://play.google.com/store/apps/details?id=com.sanctum.member"
            
            val shareText = """
                ${devotional.title}
                
                ${devotional.scriptureReference ?: ""}
                
                By ${devotional.author}
                
                ${devotional.content.take(150)}${if (devotional.content.length > 150) "..." else ""}
                
                Read more in AltarFunds App:
                $deepLink
                
                Don't have the app? Download now:
                $appStoreUrl
            """.trimIndent()
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, devotional.title)
                putExtra(Intent.EXTRA_TITLE, devotional.title)
            }
            
            // Create chooser that prioritizes our app
            val chooser = Intent.createChooser(shareIntent, "Share Devotional")
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun shareAsText(context: android.content.Context, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share as Text"))
    }
    
    private fun copyLink(context: android.content.Context, devotional: Devotional) {
        val deepLink = "altarfunds://devotional/${devotional.id}"
        
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("devotional_link", deepLink)
        clipboard.setPrimaryClip(clip)
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Link Copied")
            .setMessage("Devotional link copied to clipboard!\n\nShare this link with others to open in the app.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Devotional>() {
        override fun areItemsTheSame(oldItem: Devotional, newItem: Devotional) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Devotional, newItem: Devotional) =
            oldItem == newItem
    }
}
