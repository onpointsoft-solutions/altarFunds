package com.altarfunds.mobile.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.mobile.databinding.ItemGivingCategoryBinding
import com.altarfunds.mobile.models.GivingCategory

class GivingCategoryAdapter(
    private val onCategoryClick: (GivingCategory) -> Unit
) : ListAdapter<GivingCategory, GivingCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemGivingCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemGivingCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: GivingCategory) {
            binding.tvCategoryName.text = category.name
            binding.tvCategoryDescription.text = category.description ?: ""
            
            // Set category icon based on name
            binding.ivCategoryIcon.setImageResource(getCategoryIcon(category.name))
            
            // Set tax deductible badge
            if (category.is_tax_deductible) {
                binding.tvTaxDeductible.visibility = View.VISIBLE
            } else {
                binding.tvTaxDeductible.visibility = View.GONE
            }
            
            // Set click listener
            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }

        private fun getCategoryIcon(categoryName: String): Int {
            return when (categoryName.lowercase()) {
                "tithes" -> com.altarfunds.mobile.R.drawable.ic_tithe
                "offerings" -> com.altarfunds.mobile.R.drawable.ic_offering
                "missions" -> com.altarfunds.mobile.R.drawable.ic_missions
                "building" -> com.altarfunds.mobile.R.drawable.ic_building
                "welfare" -> com.altarfunds.mobile.R.drawable.ic_welfare
                "youth" -> com.altarfunds.mobile.R.drawable.ic_youth
                "children" -> com.altarfunds.mobile.R.drawable.ic_children
                "music" -> com.altarfunds.mobile.R.drawable.ic_music
                "media" -> com.altarfunds.mobile.R.drawable.ic_media
                "education" -> com.altarfunds.mobile.R.drawable.ic_education
                else -> com.altarfunds.mobile.R.drawable.ic_giving_default
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<GivingCategory>() {
        override fun areItemsTheSame(
            oldItem: GivingCategory, newItem: GivingCategory
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: GivingCategory, newItem: GivingCategory
        ): Boolean {
            return oldItem == newItem
        }
    }
}
