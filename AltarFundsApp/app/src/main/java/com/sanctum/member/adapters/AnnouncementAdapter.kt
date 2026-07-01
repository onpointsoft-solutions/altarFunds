package com.sanctum.member.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sanctum.member.R
import com.sanctum.member.databinding.ItemAnnouncementBinding
import com.sanctum.member.models.Announcement

class AnnouncementAdapter : ListAdapter<Announcement, AnnouncementAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((Announcement) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAnnouncementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(announcement: Announcement) {
            val ctx = binding.root.context

            binding.tvTitle.text   = announcement.title
            binding.tvContent.text = announcement.content.let {
                if (it.length > 180) it.take(180) + "…" else it
            }
            binding.tvDate.text = announcement.createdAt.take(10)

            // Church name (optional)
            binding.tvChurchName.text = announcement.churchName ?: ""

            // ── Priority pill ────────────────────────────────────────────
            val (priorityLabel, priorityColor) = when (announcement.priority.lowercase()) {
                "urgent" -> "Urgent"  to R.color.priority_urgent
                "high"   -> "High"    to R.color.priority_high
                "medium" -> "Medium"  to R.color.primary
                "low"    -> "Low"     to R.color.priority_low
                else     -> "Normal"  to R.color.primary
            }
            binding.tvPriority.text = priorityLabel
            binding.tvPriority.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, priorityColor)
                )

            // ── Left accent bar ──────────────────────────────────────────
            binding.viewPriorityBar.setBackgroundColor(
                ContextCompat.getColor(ctx, priorityColor)
            )

            binding.root.setOnClickListener { onItemClick?.invoke(announcement) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Announcement>() {
        override fun areItemsTheSame(old: Announcement, new: Announcement) = old.id == new.id
        override fun areContentsTheSame(old: Announcement, new: Announcement) = old == new
    }
}
