package com.altarfunds.mobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.mobile.R
import com.altarfunds.mobile.models.GivingTransaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter : ListAdapter<GivingTransaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "NG"))
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }
    
    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvPaymentMethod: TextView? = itemView.findViewById(R.id.tv_payment_method)
        // Note: tvReference view not in current layout
        private val tvNotes: TextView? = itemView.findViewById(R.id.tv_note)
        private val tvTime: TextView? = itemView.findViewById(R.id.tv_time)
        
        fun bind(transaction: GivingTransaction) {
            // Display category with icon
            tvCategory.text = "💰 ${transaction.category_name ?: "General Giving"}"
            
            // Display amount with proper formatting
            tvAmount.text = currencyFormat.format(transaction.amount)
            
            // Parse and format date and time
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(transaction.date)
                tvDate.text = date?.let { dateFormat.format(it) } ?: transaction.date
            } catch (e: Exception) {
                tvDate.text = transaction.date
            }
            
            // Set status with color and icon
            val statusText = transaction.status.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            tvStatus.text = when (transaction.status.lowercase()) {
                "completed", "success" -> "✓ $statusText"
                "pending" -> "⏳ $statusText"
                "failed" -> "✗ $statusText"
                else -> statusText
            }
            
            when (transaction.status.lowercase()) {
                "completed", "success" -> {
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                }
                "pending" -> {
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                }
                "failed" -> {
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                }
            }
            
            
            // Display notes if available
            tvNotes?.let { view ->
                transaction.notes?.let { notes ->
                    if (notes.isNotEmpty()) {
                        view.text = "📝 $notes"
                        view.visibility = View.VISIBLE
                    } else {
                        view.visibility = View.GONE
                    }
                } ?: run {
                    view.visibility = View.GONE
                }
            }
            
            // Add click listener for detailed view
            itemView.setOnClickListener {
                // TODO: Navigate to transaction details
            }
        }
    }
    
    class TransactionDiffCallback : DiffUtil.ItemCallback<GivingTransaction>() {
        override fun areItemsTheSame(oldItem: GivingTransaction, newItem: GivingTransaction): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: GivingTransaction, newItem: GivingTransaction): Boolean {
            return oldItem == newItem
        }
    }
}
