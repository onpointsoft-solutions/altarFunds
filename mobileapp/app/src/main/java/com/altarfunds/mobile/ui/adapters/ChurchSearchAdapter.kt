package com.altarfunds.mobile.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.altarfunds.mobile.R
import com.altarfunds.mobile.models.Church
import com.altarfunds.mobile.models.ChurchSearchResult

class ChurchSearchAdapter(
    private var churches: List<ChurchSearchResult>,
    private val onChurchClick: (ChurchSearchResult) -> Unit
) : RecyclerView.Adapter<ChurchSearchAdapter.ChurchViewHolder>() {

    class ChurchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val churchName: TextView = itemView.findViewById(R.id.church_name)
        val churchLocation: TextView = itemView.findViewById(R.id.church_location)
        val churchCode: TextView = itemView.findViewById(R.id.churchCode)
        val churchMembers: TextView = itemView.findViewById(R.id.church_members)
        val churchVerified: TextView = itemView.findViewById(R.id.tv_church_verified)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChurchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_church_search, parent, false)
        return ChurchViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChurchViewHolder, position: Int) {
        val church = churches[position]
        holder.churchName.text = church.name
        holder.churchLocation.text = "${church.location}${church.city?.let { ", $it" } ?: ""}"
        // Note: church_code field not in current model
        holder.churchCode.text = "Code: N/A"
        holder.churchMembers.text = "${church.memberCount} members"
        
        // Show verification status - Note: church_verified field not in current model
        holder.churchVerified.text = "Status Unknown"
        holder.churchVerified.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        
        holder.itemView.setOnClickListener {
            onChurchClick(church)
        }
    }

    override fun getItemCount(): Int = churches.size

    fun updateData(newChurches: List<ChurchSearchResult>) {
        churches = newChurches
        notifyDataSetChanged()
    }
}
