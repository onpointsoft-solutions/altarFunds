package com.altarfunds.mobile.data.models

import com.google.gson.annotations.SerializedName

data class Notice(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("priority")
    val priority: String, // low, medium, high
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("author")
    val author: String,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("created_at")
    val createdAt: String
)
