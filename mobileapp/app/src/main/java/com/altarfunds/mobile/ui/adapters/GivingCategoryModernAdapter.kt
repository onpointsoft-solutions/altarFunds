package com.altarfunds.mobile.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.mobile.databinding.ItemGivingCategoryModernBinding
import com.altarfunds.mobile.models.GivingCategory

class GivingCategoryModernAdapter(
    private val onCategoryClick: (GivingCategory) -> Unit
) : ListAdapter<GivingCategory, GivingCategoryModernAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGivingCategoryModernBinding.inflate(
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
        private val binding: ItemGivingCategoryModernBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: GivingCategory) {
            binding.apply {
                tvCategoryName.text = category.name
                
                // Set category icon based on category type
                //ivCategoryIcon.setImageResource(getCategoryIcon(category.name))
                
                root.setOnClickListener {
                    onCategoryClick(category)
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
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GivingCategory>() {
            override fun areItemsTheSame(
                oldItem: GivingCategory,
                newItem: GivingCategory
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: GivingCategory,
                newItem: GivingCategory
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
