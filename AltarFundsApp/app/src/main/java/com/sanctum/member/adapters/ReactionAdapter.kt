package com.sanctum.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sanctum.member.databinding.ItemReactionBinding
import com.sanctum.member.models.Reaction
import com.bumptech.glide.Glide

class ReactionAdapter : ListAdapter<Reaction, ReactionAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemReactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(reaction: Reaction) {
            binding.tvEmoji.text = reaction.emoji
            binding.tvUserName.text = reaction.userName
            
            // Load user avatar
            if (!reaction.userAvatar.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(reaction.userAvatar)
                    .circleCrop()
                    .placeholder(com.sanctum.member.R.drawable.ic_user_placeholder)
                    .into(binding.ivUserAvatar)
            } else {
                binding.ivUserAvatar.setImageResource(com.sanctum.member.R.drawable.ic_user_placeholder)
            }
            
            // Format date to show relative time
            binding.tvDate.text = formatRelativeTime(reaction.createdAt)
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
                    else -> "${diff / 86400000}d ago"
                }
            } catch (e: Exception) {
                "unknown"
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Reaction>() {
        override fun areItemsTheSame(oldItem: Reaction, newItem: Reaction) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Reaction, newItem: Reaction) =
            oldItem == newItem
    }
}
