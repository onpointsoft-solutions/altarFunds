package com.altarfunds.mobile.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.mobile.databinding.ItemRecentTransactionModernBinding
import com.altarfunds.mobile.models.GivingTransactionResponse
import com.altarfunds.mobile.utils.CurrencyUtils
import com.altarfunds.mobile.utils.DateUtils

class RecentTransactionModernAdapter(
    private val onTransactionClick: (GivingTransactionResponse) -> Unit
) : ListAdapter<GivingTransactionResponse, RecentTransactionModernAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentTransactionModernBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRecentTransactionModernBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: GivingTransactionResponse) {
            binding.apply {
                tvCategoryName.text = transaction.category.name
                tvTransactionAmount.text = CurrencyUtils.formatCurrency(transaction.amount)
                tvTransactionDate.text = DateUtils.formatShortDate(transaction.transaction_date)
                tvTransactionStatus.text = transaction.status
                
                // Set category icon
                ivCategoryIcon.setImageResource(getCategoryIcon(transaction.category.name))
                
                // Set status background based on status
                tvTransactionStatus.setBackgroundResource(getStatusBackground(transaction.status))
                
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }

        private fun getCategoryIcon(categoryName: String): Int {
            return when (categoryName.lowercase()) {
                "tithe" -> android.R.drawable.ic_menu_share
                "offering" -> android.R.drawable.ic_menu_camera
                "mission" -> android.R.drawable.ic_menu_mapmode
                "building" -> android.R.drawable.ic_menu_info_details
                "children" -> android.R.drawable.ic_menu_recent_history
                "youth" -> android.R.drawable.ic_menu_send
                else -> android.R.drawable.ic_menu_help
            }
        }

        private fun getStatusBackground(status: String): Int {
            return when (status.lowercase()) {
                "completed", "success" -> com.altarfunds.mobile.R.drawable.bg_status_success
                "pending" -> com.altarfunds.mobile.R.drawable.bg_status_success
                "failed" -> com.altarfunds.mobile.R.drawable.bg_status_success
                else -> com.altarfunds.mobile.R.drawable.bg_status_success
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GivingTransactionResponse>() {
            override fun areItemsTheSame(
                oldItem: GivingTransactionResponse,
                newItem: GivingTransactionResponse
            ): Boolean {
                return oldItem.transaction_id == newItem.transaction_id
            }

            override fun areContentsTheSame(
                oldItem: GivingTransactionResponse,
                newItem: GivingTransactionResponse
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
