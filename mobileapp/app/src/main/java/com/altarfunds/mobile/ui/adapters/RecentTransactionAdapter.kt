package com.altarfunds.mobile.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.mobile.databinding.ItemRecentTransactionBinding
import com.altarfunds.mobile.models.GivingTransactionResponse
import com.altarfunds.mobile.utils.CurrencyUtils
import com.altarfunds.mobile.utils.DateUtils

class RecentTransactionAdapter(
    private val onTransactionClick: (GivingTransactionResponse) -> Unit
) : ListAdapter<GivingTransactionResponse, RecentTransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemRecentTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemRecentTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: GivingTransactionResponse) {
            binding.tvAmount.text = CurrencyUtils.formatCurrency(transaction.amount)
            binding.tvCategory.text = transaction.category.name
            binding.tvDate.text = DateUtils.formatShortDate(transaction.transaction_date)
            binding.tvTime.text = DateUtils.formatTime(transaction.transaction_date)
            
            // Set status
            updateTransactionStatus(transaction.status)
            
            // Set payment method icon
            binding.ivPaymentMethod.setImageResource(getPaymentMethodIcon(transaction.payment_method))
            
            // Set click listener
            binding.root.setOnClickListener {
                onTransactionClick(transaction)
            }
        }

        private fun updateTransactionStatus(status: String) {
            binding.tvStatus.text = status.replace("_", " ").capitalize()
            
            val (color, background) = when (status) {
                "completed" -> Pair(
                    com.altarfunds.mobile.R.color.green,
                    com.altarfunds.mobile.R.drawable.bg_status_completed
                )
                "pending" -> Pair(
                    com.altarfunds.mobile.R.color.orange,
                    com.altarfunds.mobile.R.drawable.bg_status_pending
                )
                "failed" -> Pair(
                    com.altarfunds.mobile.R.color.red,
                    com.altarfunds.mobile.R.drawable.bg_status_failed
                )
                else -> Pair(
                    com.altarfunds.mobile.R.color.gray,
                    com.altarfunds.mobile.R.drawable.bg_status_default
                )
            }
            
            binding.tvStatus.setTextColor(binding.root.context.getColor(color))
            binding.tvStatus.setBackgroundResource(background)
        }

        private fun getPaymentMethodIcon(paymentMethod: String): Int {
            return when (paymentMethod.lowercase()) {
                "mpesa" -> com.altarfunds.mobile.R.drawable.ic_mpesa
                "cash" -> com.altarfunds.mobile.R.drawable.ic_cash
                "bank_transfer" -> com.altarfunds.mobile.R.drawable.ic_bank
                "card" -> com.altarfunds.mobile.R.drawable.ic_card
                else -> com.altarfunds.mobile.R.drawable.ic_payment_default
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<GivingTransactionResponse>() {
        override fun areItemsTheSame(
            oldItem: GivingTransactionResponse, newItem: GivingTransactionResponse
        ): Boolean {
            return oldItem.transaction_id == newItem.transaction_id
        }

        override fun areContentsTheSame(
            oldItem: GivingTransactionResponse, newItem: GivingTransactionResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
}
