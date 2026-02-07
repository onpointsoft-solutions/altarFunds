package com.altarfunds.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.member.databinding.ItemDevotionalBinding
import com.altarfunds.member.models.Devotional
import com.altarfunds.member.utils.formatDate

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
        
        fun bind(devotional: Devotional) {
            binding.tvTitle.text = devotional.title
            binding.tvScripture.text = devotional.scriptureReference
            binding.tvAuthor.text = "By ${devotional.author}"
            binding.tvDate.text = devotional.date.formatDate()
            binding.tvPreview.text = devotional.content.take(120) + if (devotional.content.length > 120) "..." else ""
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Devotional>() {
        override fun areItemsTheSame(oldItem: Devotional, newItem: Devotional) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Devotional, newItem: Devotional) =
            oldItem == newItem
    }
}
