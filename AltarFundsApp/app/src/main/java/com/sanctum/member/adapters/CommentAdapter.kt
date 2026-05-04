package com.sanctum.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sanctum.member.databinding.ItemCommentBinding
import com.sanctum.member.models.Comment
import com.bumptech.glide.Glide

class CommentAdapter : ListAdapter<Comment, CommentAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(comment: Comment) {
            binding.tvUserName.text = comment.userName
            binding.tvContent.text = comment.content
            binding.tvDate.text = formatRelativeTime(comment.createdAt)
            binding.tvLikeCount.text = comment.likeCount.toString()
            
            // Load user avatar
            if (!comment.userAvatar.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(comment.userAvatar)
                    .circleCrop()
                    .placeholder(com.sanctum.member.R.drawable.ic_user_placeholder)
                    .into(binding.ivUserAvatar)
            } else {
                binding.ivUserAvatar.setImageResource(com.sanctum.member.R.drawable.ic_user_placeholder)
            }
            
            // Like button state
            binding.btnLike.isSelected = comment.isLiked
            val likeIcon = if (comment.isLiked) {
                com.sanctum.member.R.drawable.ic_heart_filled
            } else {
                com.sanctum.member.R.drawable.ic_heart_outline
            }
            binding.btnLike.setImageResource(likeIcon)
            
            // Click listeners
            binding.btnLike.setOnClickListener {
                // Handle comment like action
            }
        }
        
        private fun formatRelativeTime(dateString: String): String {
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val date = sdf.parse(dateString)
                val now = java.util.Date()
                val diff = now.time - (date?.time ?: 0)
                
                when {
                    diff < 60000 -> "just now"
                    diff < 3600000 -> "${diff / 60000}m ago"
                    diff < 86400000 -> "${diff / 3600000}h ago"
                    diff < 604800000 -> "${diff / 86400000}d ago"
                    else -> sdf.format(date ?: java.util.Date())
                }
            } catch (e: Exception) {
                "unknown"
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) =
            oldItem == newItem
    }
}
