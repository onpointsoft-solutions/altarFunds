package com.altarfunds.mobile.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.mobile.databinding.ItemChurchBinding
import com.altarfunds.mobile.models.ChurchInfo
import com.altarfunds.mobile.models.ChurchSearchResult

class ChurchAdapter(
    private val onChurchClick: (ChurchSearchResult) -> Unit = {}
) : ListAdapter<ChurchSearchResult, ChurchAdapter.ViewHolder>(DiffCallback) {

    // Constructor for MemberDashboardActivity
    constructor(churches: List<ChurchSearchResult>) : this({}) {
        submitList(churches)
    }

    fun updateChurches(churches: List<ChurchSearchResult>) {
        submitList(churches)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChurchBinding.inflate(
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
        private val binding: ItemChurchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(church: ChurchSearchResult) {
            binding.apply {
                tvChurchName.text = church.name
                tvChurchCode.text = church.id
                tvChurchDescription.text = church.location
                
                // Set verification status (default to not verified since not available in search result)
                tvVerificationStatus.text = "Not Verified"
                tvVerificationStatus.setTextColor(android.graphics.Color.YELLOW)
                
                // Set active status (always active for search results)
                tvActiveStatus.text = "Active"
                tvActiveStatus.setTextColor(android.graphics.Color.GREEN)
                
                root.setOnClickListener {
                    onChurchClick(church)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ChurchSearchResult>() {
            override fun areItemsTheSame(
                oldItem: ChurchSearchResult,
                newItem: ChurchSearchResult
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ChurchSearchResult,
                newItem: ChurchSearchResult
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
