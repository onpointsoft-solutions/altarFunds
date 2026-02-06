package com.altarfunds.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ItemAnnouncementBinding
import com.altarfunds.member.models.Announcement
import com.altarfunds.member.utils.formatDate

class AnnouncementAdapter : ListAdapter<Announcement, AnnouncementAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemAnnouncementBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(announcement: Announcement) {
            binding.tvTitle.text = announcement.title
            binding.tvContent.text = announcement.content.take(150) + if (announcement.content.length > 150) "..." else ""
            binding.tvDate.text = announcement.createdAt.formatDate()
            binding.tvPriority.text = announcement.priorityDisplay
            
            val priorityColor = when (announcement.priority) {
                "urgent" -> R.color.priority_urgent
                "high" -> R.color.priority_high
                "medium" -> R.color.priority_medium
                "low" -> R.color.priority_low
                else -> R.color.text_secondary
            }
            binding.tvPriority.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, priorityColor)
            )
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Announcement>() {
        override fun areItemsTheSame(oldItem: Announcement, newItem: Announcement) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Announcement, newItem: Announcement) =
            oldItem == newItem
    }
}
