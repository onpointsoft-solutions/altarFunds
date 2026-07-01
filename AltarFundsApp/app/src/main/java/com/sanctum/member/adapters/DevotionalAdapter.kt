package com.sanctum.member.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sanctum.member.MemberApp
import com.sanctum.member.R
import com.sanctum.member.databinding.ItemDevotionalBinding
import com.sanctum.member.models.Devotional
import com.sanctum.member.ui.devotionals.DevotionalDetailsActivity
import com.sanctum.member.utils.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevotionalAdapter : ListAdapter<Devotional, DevotionalAdapter.ViewHolder>(DiffCallback()) {

    /** Called after a successful reaction API call so the parent can refresh counts. */
    var onDevotionalUpdated: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDevotionalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDevotionalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isLiked      = false
        private var isBookmarked = false
        private var likeCount    = 0
        private var commentCount = 0

        fun bind(devotional: Devotional) {
            val ctx = binding.root.context

            // ── Text content ───────────────────────────────────────────
            binding.tvTitle.text = devotional.title
            binding.tvScripture.text = devotional.scriptureReference ?: ""
            binding.tvDate.text = devotional.date.formatDate()
            binding.tvPreview.text = devotional.content.let {
                if (it.length > 160) it.take(160) + "…" else it
            }
            binding.tvBannerTitle.text = devotional.author.let { "By $it" }

            // ── Banner image ───────────────────────────────────────────
            if (!devotional.bannerImage.isNullOrEmpty()) {
                Glide.with(ctx)
                    .load(devotional.bannerImage)
                    .placeholder(R.drawable.bg_gradient_primary)
                    .error(R.drawable.bg_gradient_primary)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(binding.ivBanner)
            } else {
                binding.ivBanner.setImageResource(R.drawable.bg_gradient_primary)
            }

            // ── Reaction counts ────────────────────────────────────────
            likeCount    = devotional.likeCount    ?: 0
            commentCount = devotional.commentCount ?: 0
            isLiked      = devotional.isLiked
            isBookmarked = devotional.isBookmarked

            refreshLikeUI(ctx)
            refreshBookmarkUI(ctx)
            binding.tvCommentCount.text = commentCount.toString()
            binding.ivComment.setColorFilter(ContextCompat.getColor(ctx, R.color.ink_soft))
            binding.tvCommentCount.setTextColor(ContextCompat.getColor(ctx, R.color.ink_soft))
            binding.ivShare.setColorFilter(ContextCompat.getColor(ctx, R.color.ink_soft))

            // ── Click listeners ────────────────────────────────────────
            binding.root.setOnClickListener { openDetails(devotional, false) }
            binding.btnComment.setOnClickListener { openDetails(devotional, true) }

            binding.btnLike.setOnClickListener {
                val devotionalId = devotional.id ?: return@setOnClickListener
                isLiked = !isLiked
                likeCount = if (isLiked) likeCount + 1 else maxOf(0, likeCount - 1)
                refreshLikeUI(ctx)

                // Use the RecyclerView's lifecycle scope (no GlobalScope leak)
                val scope = binding.root.findViewTreeLifecycleOwner()?.lifecycleScope
                    ?: return@setOnClickListener

                scope.launch {
                    runCatching {
                        val app = MemberApp.getInstance()
                        if (isLiked) {
                            app.apiService.reactToDevotional(
                                devotionalId, mapOf("reaction_type" to "love")
                            )
                        } else {
                            app.apiService.removeDevotionalReaction(devotionalId, "love")
                        }
                    }.onFailure {
                        // Revert on error
                        withContext(Dispatchers.Main) {
                            isLiked = !isLiked
                            likeCount = if (isLiked) likeCount + 1 else maxOf(0, likeCount - 1)
                            refreshLikeUI(ctx)
                            Toast.makeText(ctx, "Could not save reaction", Toast.LENGTH_SHORT).show()
                        }
                    }.onSuccess {
                        withContext(Dispatchers.Main) { onDevotionalUpdated?.invoke() }
                    }
                }
            }

            binding.ivBookmark.setOnClickListener {
                val devotionalId = devotional.id ?: return@setOnClickListener
                isBookmarked = !isBookmarked
                refreshBookmarkUI(ctx)

                val scope = binding.root.findViewTreeLifecycleOwner()?.lifecycleScope
                    ?: return@setOnClickListener

                scope.launch {
                    runCatching {
                        MemberApp.getInstance().apiService.bookmarkDevotional(devotionalId)
                    }.onFailure {
                        withContext(Dispatchers.Main) {
                            isBookmarked = !isBookmarked
                            refreshBookmarkUI(ctx)
                            Toast.makeText(ctx, "Could not save bookmark", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            binding.ivShare.setOnClickListener {
                val scripture = devotional.scriptureReference ?: ""
                val preview   = devotional.content.take(200).let {
                    if (it.length >= 200) "$it…" else it
                }
                val text = buildString {
                    appendLine("\"${devotional.title}\"")
                    if (scripture.isNotEmpty()) appendLine("📖 $scripture")
                    appendLine("✍️ By ${devotional.author}")
                    appendLine()
                    appendLine(preview)
                    appendLine()
                    append("Read more in the AltarFunds App")
                }
                ctx.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, devotional.title)
                            putExtra(Intent.EXTRA_TEXT, text)
                        },
                        "Share Devotional"
                    )
                )
            }
        }

        private fun openDetails(devotional: Devotional, openComments: Boolean) {
            binding.root.context.startActivity(
                Intent(binding.root.context, DevotionalDetailsActivity::class.java).apply {
                    putExtra("devotional_id", devotional.id)
                    putExtra("open_comments", openComments)
                }
            )
        }

        private fun refreshLikeUI(ctx: android.content.Context) {
            binding.tvLikeCount.text = likeCount.toString()
            if (isLiked) {
                binding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                binding.ivLike.setColorFilter(ContextCompat.getColor(ctx, R.color.primary))
                binding.tvLikeCount.setTextColor(ContextCompat.getColor(ctx, R.color.primary))
            } else {
                binding.ivLike.setImageResource(R.drawable.ic_heart_outline)
                binding.ivLike.setColorFilter(ContextCompat.getColor(ctx, R.color.ink_soft))
                binding.tvLikeCount.setTextColor(ContextCompat.getColor(ctx, R.color.ink_soft))
            }
        }

        private fun refreshBookmarkUI(ctx: android.content.Context) {
            if (isBookmarked) {
                binding.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                binding.ivBookmark.setColorFilter(ContextCompat.getColor(ctx, R.color.primary))
            } else {
                binding.ivBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                binding.ivBookmark.setColorFilter(ContextCompat.getColor(ctx, R.color.ink_soft))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Devotional>() {
        override fun areItemsTheSame(old: Devotional, new: Devotional) = old.id == new.id
        override fun areContentsTheSame(old: Devotional, new: Devotional) = old == new
    }
}
