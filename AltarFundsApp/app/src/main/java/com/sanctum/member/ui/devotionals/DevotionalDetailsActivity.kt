package com.sanctum.member.ui.devotionals

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sanctum.member.MemberApp
import com.sanctum.member.adapters.CommentAdapter
import com.sanctum.member.adapters.ReactionAdapter
import com.sanctum.member.R
import com.sanctum.member.databinding.ActivityDevotionalDetailsBinding
import com.sanctum.member.utils.*
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class DevotionalDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDevotionalDetailsBinding
    private val app by lazy { MemberApp.getInstance() }
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var reactionAdapter: ReactionAdapter
    private var devotionalId: Int = -1
    private var userReactions: MutableSet<String> = mutableSetOf()

    private val reactionTypes = mapOf(
        R.id.chipLove      to Pair("love",      "\u2764\uFE0F"),
        R.id.chipPray      to Pair("pray",      "\uD83D\uDE4F"),
        R.id.chipThumbsUp  to Pair("thumbs_up", "\uD83D\uDC4D"),
        R.id.chipFire      to Pair("fire",      "\uD83D\uDD25"),
        R.id.chipCelebrate to Pair("celebrate", "\uD83C\uDF89")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDevotionalDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()

        devotionalId = intent.getIntExtra("devotional_id", -1)
        if (devotionalId == -1) { finish(); return }

        loadDevotionalDetails()

        if (intent.getBooleanExtra("open_comments", false)) {
            // Scroll to comments section after load
            binding.root.post {
                binding.etComment.requestFocus()
            }
        }
    }

    // ── Toolbar ──────────────────────────────────────────────────────────

    private fun setupToolbar() {
        // Use custom back button — don't call setSupportActionBar to avoid
        // the default home-as-up icon conflicting with our custom btnBack.
        binding.btnBack.setOnClickListener { finish() }
        binding.btnMore.setOnClickListener {
            // Optional: show popup menu
        }
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────

    private fun setupRecyclerViews() {
        commentAdapter = CommentAdapter()
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@DevotionalDetailsActivity)
            adapter = commentAdapter
            isNestedScrollingEnabled = false
        }

        reactionAdapter = ReactionAdapter()
        binding.rvReactions.apply {
            layoutManager = LinearLayoutManager(
                this@DevotionalDetailsActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = reactionAdapter
        }
    }

    // ── Load devotional ───────────────────────────────────────────────────

    private fun loadDevotionalDetails() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionalDetails(devotionalId)
                if (response.isSuccessful && response.body() != null) {
                    displayDevotional(response.body()!!)
                } else {
                    showToast("Failed to load devotional")
                    finish()
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
                finish()
            }
        }
    }

    // ── Display ───────────────────────────────────────────────────────────

    private fun displayDevotional(devotional: com.sanctum.member.models.Devotional) {
        // Banner
        if (!devotional.bannerImage.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(devotional.bannerImage)
                .placeholder(R.drawable.bg_gradient_primary)
                .error(R.drawable.bg_gradient_primary)
                .centerCrop()
                .into(binding.ivBanner)
        } else {
            binding.ivBanner.setImageResource(R.drawable.bg_gradient_primary)
        }

        // Banner text
        binding.tvBannerTitle.text    = devotional.title
        binding.tvBannerSubtitle.text = "By ${devotional.author}"

        // Body
        binding.tvTitle.text     = devotional.title
        binding.tvScripture.text = devotional.scriptureReference ?: ""
        binding.tvDate.text      = devotional.date.formatDate()
        binding.tvContent.text   = devotional.content

        // Counts
        binding.tvLikeCount.text    = (devotional.likeCount ?: 0).toString()
        binding.tvCommentCount.text = (devotional.commentCount ?: 0).toString()

        val total = devotional.reactionsCount ?: 0
        if (total > 0) {
            binding.tvReactionCount.text = "$total reactions"
            binding.tvReactionCount.visible()
        } else {
            binding.tvReactionCount.gone()
        }

        // Bookmark state
        updateBookmarkIcon(devotional.isBookmarked)

        // Like state
        updateLikeIcon(devotional.isLiked)

        // User's current reaction chips
        devotional.userReaction?.let { rt ->
            userReactions.clear()
            userReactions.add(rt)
            updateReactionChipState(rt, true)
        }

        setupClickListeners(devotional)

        // Load reactions and comments in parallel
        lifecycleScope.launch { loadReactions() }
        lifecycleScope.launch { loadComments() }
    }

    // ── Click listeners ───────────────────────────────────────────────────

    private fun setupClickListeners(devotional: com.sanctum.member.models.Devotional) {
        binding.btnShare.setOnClickListener   { shareDevotional(devotional) }
        binding.btnLike.setOnClickListener    { toggleLike(devotional.id ?: -1) }
        binding.btnBookmark.setOnClickListener { toggleBookmark(devotional.id ?: -1) }
        binding.btnComment.setOnClickListener {
            binding.etComment.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etComment, InputMethodManager.SHOW_IMPLICIT)
        }
        binding.btnSendComment.setOnClickListener { postComment(devotional.id ?: -1) }
        setupEmojiReactions()
    }

    // ── Emoji chips ───────────────────────────────────────────────────────

    private fun setupEmojiReactions() {
        reactionTypes.forEach { (chipId, pair) ->
            val (reactionType, emoji) = pair
            val chip = findViewById<Chip>(chipId) ?: return@forEach
            chip.setOnClickListener { toggleReaction(reactionType, emoji) }
        }
    }

    private fun toggleReaction(reactionType: String, emoji: String) {
        if (devotionalId == -1) return
        val isAdding = !userReactions.contains(reactionType)

        lifecycleScope.launch {
            try {
                if (isAdding) {
                    val response = app.apiService.likeDevotional(
                        devotionalId, mapOf("reaction_type" to reactionType)
                    )
                    if (response.isSuccessful) {
                        userReactions.add(reactionType)
                        updateReactionChipState(reactionType, true)
                        response.body()?.let {
                            binding.tvLikeCount.text = it.likeCount.toString()
                        }
                        showToast("Reacted with $emoji")
                        loadReactions()
                    } else {
                        showToast("Failed to add reaction")
                    }
                } else {
                    val response = app.apiService.removeDevotionalReaction(devotionalId, reactionType)
                    if (response.isSuccessful) {
                        userReactions.remove(reactionType)
                        updateReactionChipState(reactionType, false)
                        showToast("Removed $emoji reaction")
                        loadReactions()
                    } else {
                        showToast("Failed to remove reaction")
                    }
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
            }
        }
    }

    private fun updateReactionChipState(reactionType: String, isSelected: Boolean) {
        val entry = reactionTypes.entries.find { it.value.first == reactionType } ?: return
        val chip  = findViewById<Chip>(entry.key) ?: return
        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(
                this,
                if (isSelected) R.color.chip_selected else R.color.chip_unselected
            )
        )
    }

    // ── Like ──────────────────────────────────────────────────────────────

    private fun toggleLike(id: Int) {
        if (id == -1) return
        lifecycleScope.launch {
            try {
                val response = app.apiService.likeDevotional(id, mapOf("reaction_type" to "love"))
                if (response.isSuccessful && response.body() != null) {
                    val r = response.body()!!
                    binding.tvLikeCount.text = r.likeCount.toString()
                    updateLikeIcon(r.isLiked)
                    showToast(r.message)
                } else {
                    showToast("Failed to like")
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
            }
        }
    }

    private fun updateLikeIcon(liked: Boolean) {
        binding.btnLike.setImageResource(
            if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        binding.btnLike.setColorFilter(
            ContextCompat.getColor(
                this,
                if (liked) R.color.primary else R.color.gold_primary
            )
        )
    }

    // ── Bookmark ──────────────────────────────────────────────────────────

    private fun toggleBookmark(id: Int) {
        if (id == -1) return
        lifecycleScope.launch {
            try {
                val response = app.apiService.bookmarkDevotional(id)
                if (response.isSuccessful && response.body() != null) {
                    val r = response.body()!!
                    updateBookmarkIcon(r.isBookmarked)
                    showToast(r.message)
                } else {
                    showToast("Failed to bookmark")
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
            }
        }
    }

    private fun updateBookmarkIcon(bookmarked: Boolean) {
        binding.btnBookmark.setImageResource(
            if (bookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        )
        binding.btnBookmark.setColorFilter(
            ContextCompat.getColor(
                this,
                if (bookmarked) R.color.primary else R.color.gold_primary
            )
        )
    }

    // ── Comments ──────────────────────────────────────────────────────────

    private fun loadComments() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionalComments(devotionalId)
                if (response.isSuccessful && response.body() != null) {
                    val comments = response.body()!!
                    commentAdapter.submitList(comments)
                    binding.tvCommentCount.text = comments.size.toString()
                    if (comments.isNotEmpty()) {
                        binding.tvNoComments.gone()
                        binding.rvComments.visible()
                    } else {
                        binding.tvNoComments.visible()
                        binding.rvComments.gone()
                    }
                } else {
                    binding.tvNoComments.visible()
                    binding.rvComments.gone()
                }
            } catch (e: Exception) {
                binding.tvNoComments.visible()
                binding.rvComments.gone()
            }
        }
    }

    private fun postComment(id: Int) {
        val text = binding.etComment.text.toString().trim()
        if (text.isEmpty()) { showToast("Please enter a comment"); return }

        binding.btnSendComment.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = app.apiService.postComment(
                    id, mapOf("content" to text, "devotional" to id.toString())
                )
                if (response.isSuccessful && response.body() != null) {
                    binding.etComment.text.clear()
                    val list = commentAdapter.currentList.toMutableList()
                    list.add(0, response.body()!!)
                    commentAdapter.submitList(list)
                    binding.tvNoComments.gone()
                    binding.rvComments.visible()
                    val count = binding.tvCommentCount.text.toString().toIntOrNull() ?: 0
                    binding.tvCommentCount.text = (count + 1).toString()
                    showToast("Comment posted")
                    // Dismiss keyboard
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.etComment.windowToken, 0)
                } else {
                    showToast("Failed to post comment (${response.code()})")
                }
            } catch (e: Exception) {
                showToast("Network error: ${e.message}")
            } finally {
                binding.btnSendComment.isEnabled = true
            }
        }
    }

    // ── Reactions list ────────────────────────────────────────────────────

    private fun loadReactions() {
        lifecycleScope.launch {
            try {
                val response = app.apiService.getDevotionalReactions(devotionalId)
                if (response.isSuccessful && response.body() != null) {
                    val reactions = response.body()!!
                    reactionAdapter.submitList(reactions)

                    // Update total reaction count badge
                    if (reactions.isNotEmpty()) {
                        binding.tvReactionCount.text = "${reactions.size} reaction${if (reactions.size != 1) "s" else ""}"
                        binding.tvReactionCount.visible()
                    }

                    // Highlight chips the current user has already reacted with
                    val userEmail = app.tokenManager.getUserEmail()
                    userReactions.clear()
                    reactions.forEach { r ->
                        if (r.userName == userEmail) userReactions.add(r.reactionType)
                    }
                    reactionTypes.values.forEach { (rt, _) ->
                        updateReactionChipState(rt, userReactions.contains(rt))
                    }
                }
            } catch (_: Exception) { /* ignore — not critical */ }
        }
    }

    // ── Share ─────────────────────────────────────────────────────────────

    private fun shareDevotional(devotional: com.sanctum.member.models.Devotional) {
        val preview = devotional.content.take(200).let { if (it.length >= 200) "$it\u2026" else it }
        val text = buildString {
            appendLine("\"${devotional.title}\"")
            devotional.scriptureReference?.let { appendLine("\uD83D\uDCD6 $it") }
            appendLine("\u270D\uFE0F By ${devotional.author}")
            appendLine()
            appendLine(preview)
            appendLine()
            append("Read more in the AltarFunds App")
        }
        startActivity(
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

    // ── Emoji helper ──────────────────────────────────────────────────────

    private fun getEmojiForReaction(reactionType: String): String = when (reactionType) {
        "love"      -> "\u2764\uFE0F"
        "pray"      -> "\uD83D\uDE4F"
        "thumbs_up" -> "\uD83D\uDC4D"
        "fire"      -> "\uD83D\uDD25"
        "celebrate" -> "\uD83C\uDF89"
        else        -> "\u2764\uFE0F"
    }
}
