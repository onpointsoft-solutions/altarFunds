package com.altarfunds.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ItemDonationBinding
import com.altarfunds.member.models.Donation
import com.altarfunds.member.utils.formatCurrency
import com.altarfunds.member.utils.formatDate

class DonationAdapter : ListAdapter<Donation, DonationAdapter.ViewHolder>(DiffCallback()) {
    
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
        
        fun bind(donation: Donation) {
            binding.tvAmount.text = donation.amount.formatCurrency()
            binding.tvType.text = donation.donationTypeDisplay
            binding.tvDate.text = donation.createdAt.formatDate()
            binding.tvStatus.text = donation.statusDisplay
            
            val statusColor = when (donation.status) {
                "completed" -> R.color.success
                "pending" -> R.color.warning
                "failed" -> R.color.error
                else -> R.color.text_secondary
            }
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(binding.root.context, statusColor)
            )
            
            val typeColor = when (donation.donationType) {
                "tithe" -> R.color.tithe_color
                "offering" -> R.color.offering_color
                "special_offering" -> R.color.special_offering_color
                else -> R.color.primary
            }
            binding.cardDonation.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, typeColor)
            )
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<Donation>() {
        override fun areItemsTheSame(oldItem: Donation, newItem: Donation) =
            oldItem.id == newItem.id
        
        override fun areContentsTheSame(oldItem: Donation, newItem: Donation) =
            oldItem == newItem
    }
}
