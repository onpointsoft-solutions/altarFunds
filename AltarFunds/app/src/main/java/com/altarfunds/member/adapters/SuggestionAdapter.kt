package com.altarfunds.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.member.databinding.ItemSuggestionBinding
import com.altarfunds.member.models.Suggestion
import com.altarfunds.member.utils.gone
import com.altarfunds.member.utils.visible

class SuggestionAdapter : ListAdapter<Suggestion, SuggestionAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSuggestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemSuggestionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(suggestion: Suggestion) {
            binding.tvTitle.text = suggestion.title
            binding.tvDescription.text = suggestion.description
            binding.tvCategory.text = suggestion.categoryDisplay
            binding.tvStatus.text = suggestion.statusDisplay
            binding.tvDate.text = suggestion.createdAt
            
            // Set status color
            val statusColor = when (suggestion.status) {
                "pending" -> android.R.color.holo_orange_dark
                "reviewed" -> android.R.color.holo_blue_dark
                "implemented" -> android.R.color.holo_green_dark
                "rejected" -> android.R.color.holo_red_dark
                else -> android.R.color.darker_gray
            }
            binding.tvStatus.setTextColor(binding.root.context.getColor(statusColor))
            
            // Show response if available
            if (suggestion.response != null && suggestion.response.isNotEmpty()) {
                binding.cardResponse.visible()
                binding.tvResponse.text = suggestion.response
                binding.tvReviewedBy.text = "Response by ${suggestion.reviewedByName ?: "Pastor"}"
            } else {
                binding.cardResponse.gone()
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Suggestion>() {
        override fun areItemsTheSame(oldItem: Suggestion, newItem: Suggestion) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Suggestion, newItem: Suggestion) =
            oldItem == newItem
    }
}
