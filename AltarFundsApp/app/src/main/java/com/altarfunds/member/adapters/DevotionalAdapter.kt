package com.altarfunds.member.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.member.R
import com.altarfunds.member.models.Devotional
import com.altarfunds.member.utils.formatDate
import com.altarfunds.member.databinding.ItemDevotionalBinding
import com.altarfunds.member.ui.devotionals.DevotionalDetailsActivity

class DevotionalAdapter : ListAdapter<Devotional, DevotionalAdapter.ViewHolder>(DiffCallback()) {
    
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
                // You can implement image loading logic here
                // For now, using a gradient background based on title
                val bannerColors = intArrayOf(
                    ContextCompat.getColor(binding.root.context, R.color.primary),
                    ContextCompat.getColor(binding.root.context, R.color.secondary),
                    ContextCompat.getColor(binding.root.context, R.color.accent)
                )
                val gradient = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    bannerColors
                )
                gradient.cornerRadius = 16f
                binding.ivBanner.background = gradient
                
                // Add title overlay on banner
                // Note: tvBannerTitle doesn't exist in item_devotional.xml layout
                // This would need to be added to the layout if needed
            } else {
                binding.ivBanner.setImageResource(R.drawable.ic_devotional_placeholder)
            }
            
            // Set real reaction counts (remove mock data)
            likeCount = devotional.likeCount ?: 0
            commentCount = devotional.commentCount ?: 0
            updateReactionCounts()
            
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
                isLiked = !isLiked
                if (isLiked) {
                    likeCount++
                    binding.ivLike.setImageResource(com.altarfunds.member.R.drawable.ic_heart_filled)
                    binding.ivLike.setColorFilter(ContextCompat.getColor(binding.root.context, com.altarfunds.member.R.color.primary))
                } else {
                    likeCount--
                    binding.ivLike.setImageResource(com.altarfunds.member.R.drawable.ic_heart_outline)
                    binding.ivLike.setColorFilter(ContextCompat.getColor(binding.root.context, com.altarfunds.member.R.color.text_secondary))
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
                    binding.ivBookmark.setImageResource(com.altarfunds.member.R.drawable.ic_bookmark_filled)
                    binding.ivBookmark.setColorFilter(ContextCompat.getColor(binding.root.context, com.altarfunds.member.R.color.primary))
                } else {
                    binding.ivBookmark.setImageResource(com.altarfunds.member.R.drawable.ic_bookmark_outline)
                    binding.ivBookmark.setColorFilter(ContextCompat.getColor(binding.root.context, com.altarfunds.member.R.color.text_secondary))
                }
            }
        }
        
        private fun updateReactionCounts() {
            binding.tvLikeCount.text = likeCount.toString()
            binding.tvCommentCount.text = commentCount.toString()
        }
    }
    
    private fun shareWithApp(context: android.content.Context, devotional: Devotional) {
        try {
            // Generate deep link for app
            val deepLink = "altarfunds://devotional/${devotional.id}"
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${devotional.title}\n\n${devotional.scriptureReference ?: ""}\n\nBy ${devotional.author}\n\nRead more in AltarFunds App: $deepLink")
                putExtra(Intent.EXTRA_SUBJECT, devotional.title)
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
