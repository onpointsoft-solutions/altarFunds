package com.altarfunds.member.utils

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.altarfunds.member.R

class EmptyStateHelper(
    private val container: android.view.ViewGroup,
    private val onRefreshAction: (() -> Unit)? = null
) {
    
    private val emptyStateView: View = LayoutInflater.from(container.context)
        .inflate(R.layout.empty_state_layout, container, false)
    
    private val ivIcon: ImageView = emptyStateView.findViewById(R.id.ivEmptyIcon)
    private val tvTitle: TextView = emptyStateView.findViewById(R.id.tvEmptyTitle)
    private val tvDescription: TextView = emptyStateView.findViewById(R.id.tvEmptyDescription)
    private val btnAction: Button = emptyStateView.findViewById(R.id.btnEmptyAction)
    
    init {
        // Set up refresh button if action is provided
        onRefreshAction?.let {
            btnAction.visibility = View.VISIBLE
            btnAction.setOnClickListener {
                it()
            }
        }
    }
    
    fun show(
        iconRes: Int = R.drawable.ic_empty_state,
        title: String = "No Data Available",
        description: String = "Check back later or refresh to see new content",
        actionText: String? = null
    ) {
        // Update content
        ivIcon.setImageResource(iconRes)
        tvTitle.text = title
        tvDescription.text = description
        
        // Update action button
        actionText?.let {
            btnAction.text = it
            btnAction.visibility = View.VISIBLE
        } ?: run {
            btnAction.visibility = View.GONE
        }
        
        // Add to container if not already added
        if (emptyStateView.parent == null) {
            container.addView(emptyStateView)
        }
        
        // Hide other children
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child != emptyStateView) {
                child.visibility = View.GONE
            }
        }
        
        emptyStateView.visibility = View.VISIBLE
    }
    
    fun hide() {
        emptyStateView.visibility = View.GONE
        
        // Show other children
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child != emptyStateView) {
                child.visibility = View.VISIBLE
            }
        }
        
        // Remove from parent
        (emptyStateView.parent as? android.view.ViewGroup)?.removeView(emptyStateView)
    }
    
    companion object {
        fun forGiving(container: android.view.ViewGroup, onRefresh: () -> Unit): EmptyStateHelper {
            return EmptyStateHelper(container, onRefresh).apply {
                show(
                    iconRes = R.drawable.ic_empty_state,
                    title = "No Giving History",
                    description = "You haven't made any donations yet. Tap the button below to give.",
                    actionText = "Give Now"
                )
            }
        }
        
        fun forAnnouncements(container: android.view.ViewGroup, onRefresh: () -> Unit): EmptyStateHelper {
            return EmptyStateHelper(container, onRefresh).apply {
                show(
                    iconRes = R.drawable.ic_empty_state,
                    title = "No Announcements",
                    description = "No announcements available at the moment. Check back later.",
                    actionText = "Refresh"
                )
            }
        }
        
        fun forDevotionals(container: android.view.ViewGroup, onRefresh: () -> Unit): EmptyStateHelper {
            return EmptyStateHelper(container, onRefresh).apply {
                show(
                    iconRes = R.drawable.ic_empty_state,
                    title = "No Devotionals",
                    description = "No devotionals available at the moment. Check back later.",
                    actionText = "Refresh"
                )
            }
        }
        
        fun forNetworkError(container: android.view.ViewGroup, onRetry: () -> Unit): EmptyStateHelper {
            return EmptyStateHelper(container, onRetry).apply {
                show(
                    iconRes = android.R.drawable.stat_notify_error,
                    title = "No Internet Connection",
                    description = "Please check your network connection and try again.",
                    actionText = "Retry"
                )
            }
        }
    }
}
