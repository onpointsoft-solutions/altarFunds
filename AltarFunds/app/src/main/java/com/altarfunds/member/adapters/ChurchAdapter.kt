package com.altarfunds.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.member.databinding.ItemChurchBinding
import com.altarfunds.member.models.Church

class ChurchAdapter(
    private val onChurchClick: (Church) -> Unit
) : ListAdapter<Church, ChurchAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChurchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemChurchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(church: Church) {
            binding.tvName.text = church.name
            binding.tvCode.text = "Code: ${church.code}"
            binding.tvDenomination.text = church.denominationName ?: "No denomination"
            binding.tvLocation.text = church.city ?: "No location"
            
            binding.root.setOnClickListener {
                onChurchClick(church)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Church>() {
        override fun areItemsTheSame(oldItem: Church, newItem: Church) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Church, newItem: Church) =
            oldItem == newItem
    }
}
