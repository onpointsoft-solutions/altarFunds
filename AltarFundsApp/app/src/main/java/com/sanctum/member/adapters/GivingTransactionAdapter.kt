package com.sanctum.member.adapters

import android.app.ProgressDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sanctum.member.R
import com.sanctum.member.databinding.ItemDonationBinding
import com.sanctum.member.models.GivingTransaction
import com.sanctum.member.utils.formatCurrency
import com.sanctum.member.utils.formatDate
import com.sanctum.member.MemberApp
import com.sanctum.member.ui.giving.PaystackWebViewActivity
import kotlinx.coroutines.launch

class GivingTransactionAdapter :
    ListAdapter<GivingTransaction, GivingTransactionAdapter.ViewHolder>(DiffCallback()) {

    /** Called after a payment completes so the parent refreshes the list. */
    var onPaymentCompleted: (() -> Unit)? = null

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
            val ctx = binding.root.context

            binding.tvAmount.text = transaction.amount.formatCurrency()
            binding.tvType.text   = transaction.category.name.ifEmpty { "General" }
            binding.tvDate.text   = transaction.createdAt.formatDate()
            binding.tvStatus.text = transaction.status.replaceFirstChar { it.uppercase() }

            val statusColor = when (transaction.status.lowercase()) {
                "completed" -> R.color.success
                "pending"   -> R.color.warning
                "failed"    -> R.color.error
                else        -> R.color.text_secondary
            }
            binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, statusColor))

            // "Complete Payment" button — only visible on pending transactions
            val isPending = transaction.status.lowercase() == "pending"
            binding.btnCompletePayment.visibility =
                if (isPending) android.view.View.VISIBLE else android.view.View.GONE

            binding.btnCompletePayment.setOnClickListener {
                initiateRetryPayment(transaction)
            }

            binding.cardDonation.setCardBackgroundColor(
                ContextCompat.getColor(
                    ctx,
                    when (transaction.status.lowercase()) {
                        "completed" -> R.color.primary
                        "pending"   -> R.color.text_secondary
                        else        -> R.color.error
                    }
                )
            )
        }

        private fun initiateRetryPayment(transaction: GivingTransaction) {
            val ctx   = binding.root.context
            val scope = binding.root.findViewTreeLifecycleOwner()?.lifecycleScope ?: return
            val txId  = transaction.transactionId

            val progress = ProgressDialog(ctx).apply {
                setMessage("Initializing payment…")
                setCancelable(false)
                show()
            }

            scope.launch {
                runCatching {
                    MemberApp.getInstance().apiService.retryGivingPayment(txId)
                }.onSuccess { response ->
                    progress.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        val body   = response.body()!!
                        val authUrl = body.authorizationUrl
                        val ref    = body.paymentReference ?: ""
                        if (!authUrl.isNullOrBlank()) {
                            // Find the activity to launch the WebView
                            var activity = ctx
                            while (activity is android.content.ContextWrapper &&
                                   activity !is android.app.Activity) {
                                activity = activity.baseContext
                            }
                            if (activity is androidx.activity.ComponentActivity) {
                                activity.startActivity(
                                    PaystackWebViewActivity.newIntent(
                                        context   = ctx,
                                        authUrl   = authUrl,
                                        reference = ref,
                                        txId      = txId,
                                    )
                                )
                                onPaymentCompleted?.invoke()   // will refresh after WebView returns
                            }
                        } else {
                            android.widget.Toast.makeText(
                                ctx, body.message ?: "Could not get payment URL", android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        android.widget.Toast.makeText(
                            ctx, "Payment initialization failed (${response.code()})", android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onFailure { e ->
                    progress.dismiss()
                    android.widget.Toast.makeText(ctx, "Network error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<GivingTransaction>() {
        override fun areItemsTheSame(old: GivingTransaction, new: GivingTransaction) =
            old.id == new.id
        override fun areContentsTheSame(old: GivingTransaction, new: GivingTransaction) =
            old == new
    }
}
