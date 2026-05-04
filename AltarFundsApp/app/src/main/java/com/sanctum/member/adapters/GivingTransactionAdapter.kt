package com.sanctum.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sanctum.member.R
import com.sanctum.member.databinding.ItemDonationBinding
import com.sanctum.member.models.GivingTransaction
import com.sanctum.member.utils.formatCurrency
import com.sanctum.member.utils.formatDate

class GivingTransactionAdapter : ListAdapter<GivingTransaction, GivingTransactionAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDonationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(private val binding: ItemDonationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(transaction: GivingTransaction) {
            binding.tvAmount.text = transaction.amount.formatCurrency()
            binding.tvType.text = transaction.category.name
            binding.tvDate.text = transaction.createdAt.formatDate()
            binding.tvStatus.text = transaction.status.capitalize()
            
            val statusColor = when (transaction.status.lowercase()) {
                "completed" -> R.color.success
                "pending" -> R.color.warning
                "failed" -> R.color.error
                else -> R.color.text_secondary
            }
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, statusColor)
            )
            
            // Use primary color for all giving transactions
            binding.cardDonation.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, R.color.primary)
            )
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<GivingTransaction>() {
        override fun areItemsTheSame(oldItem: GivingTransaction, newItem: GivingTransaction) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: GivingTransaction, newItem: GivingTransaction) =
            oldItem == newItem
    }
}
