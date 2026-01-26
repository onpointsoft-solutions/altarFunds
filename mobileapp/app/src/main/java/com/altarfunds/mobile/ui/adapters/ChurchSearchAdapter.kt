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
        val churchMembers: TextView = itemView.findViewById(R.id.church_members)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChurchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_church_search, parent, false)
        return ChurchViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChurchViewHolder, position: Int) {
        val church = churches[position]
        holder.churchName.text = church.name
        holder.churchLocation.text = church.location
        holder.churchMembers.text = "${church.memberCount} members"
        
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
