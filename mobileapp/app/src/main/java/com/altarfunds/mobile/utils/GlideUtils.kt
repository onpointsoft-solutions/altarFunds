package com.altarfunds.mobile.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

object GlideUtils {
    
    fun loadImage(context: Context, imageUrl: String?, imageView: ImageView) {
        if (imageUrl.isNullOrEmpty()) {
            // Load placeholder or default image
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }
        
        Glide.with(context)
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_camera)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
            )
            .into(imageView)
    }
    
    fun loadImageWithPlaceholder(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        placeholderRes: Int
    ) {
        if (imageUrl.isNullOrEmpty()) {
            imageView.setImageResource(placeholderRes)
            return
        }
        
        Glide.with(context)
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
            )
            .into(imageView)
    }
}
