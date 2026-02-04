package com.altarfunds.mobile.data.models

import com.google.gson.annotations.SerializedName

data class Devotional(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("scripture_reference")
    val scriptureReference: String?,
    
    @SerializedName("author_name")
    val authorName: String,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("comments_count")
    val commentsCount: Int = 0,
    
    @SerializedName("reactions_count")
    val reactionsCount: Int = 0,
    
    @SerializedName("user_reaction")
    val userReaction: String?,
    
    @SerializedName("created_at")
    val createdAt: String
)

data class DevotionalComment(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("user_name")
    val userName: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("created_at")
    val createdAt: String
)

data class DevotionalReaction(
    @SerializedName("reaction_type")
    val reactionType: String
)
