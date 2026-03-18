package com.altarfunds.member.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.net.URL
import java.io.IOException
import java.net.UnknownHostException
import java.net.SocketTimeoutException

object ImageLoader {
    
    private val imageCache = mutableMapOf<String, Bitmap>()
    private val loadingJobs = mutableMapOf<String, Job>()
    
    fun loadImage(
        context: Context,
        imageView: ImageView,
        url: String?,
        placeholderRes: Int = android.R.drawable.ic_menu_gallery,
        errorRes: Int = android.R.drawable.ic_menu_report_image
    ) {
        // Cancel previous loading job for this image view
        loadingJobs[imageView.hashCode().toString()]?.cancel()
        
        // Set placeholder immediately
        imageView.setImageDrawable(ContextCompat.getDrawable(context, placeholderRes))
        
        if (url.isNullOrEmpty()) {
            imageView.setImageDrawable(ContextCompat.getDrawable(context, errorRes))
            return
        }
        
        // Check cache first
        imageCache[url]?.let { cachedBitmap ->
            imageView.setImageBitmap(cachedBitmap)
            return
        }
        
        // Load image in background
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = downloadBitmap(url)
                withContext(Dispatchers.Main) {
                    if (isActive) { // Check if job wasn't cancelled
                        imageCache[url] = bitmap
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        imageView.setImageDrawable(ContextCompat.getDrawable(context, errorRes))
                    }
                }
            }
        }
        
        loadingJobs[imageView.hashCode().toString()] = job
        
        // Clean up job when view is detached
        imageView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                loadingJobs[imageView.hashCode().toString()]?.cancel()
                loadingJobs.remove(imageView.hashCode().toString())
                imageView.removeOnAttachStateChangeListener(this)
            }
        })
    }
    
    private suspend fun downloadBitmap(urlString: String): Bitmap = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.doInput = true
            connection.connect()
            val inputStream = connection.getInputStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: UnknownHostException) {
            throw IOException("Host not found: ${e.message}")
        } catch (e: SocketTimeoutException) {
            throw IOException("Connection timeout: ${e.message}")
        } catch (e: IOException) {
            throw IOException("Network error: ${e.message}")
        }
    }
    
    fun clearCache() {
        imageCache.clear()
        loadingJobs.values.forEach { it.cancel() }
        loadingJobs.clear()
    }
    
    fun preloadImages(context: Context, urls: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            urls.forEach { url ->
                if (!imageCache.containsKey(url)) {
                    try {
                        val bitmap = downloadBitmap(url)
                        imageCache[url] = bitmap
                    } catch (e: Exception) {
                        // Ignore errors during preloading
                    }
                }
            }
        }
    }
}

// Extension function for easier usage
fun ImageView.loadImage(
    url: String?,
    placeholderRes: Int = android.R.drawable.ic_menu_gallery,
    errorRes: Int = android.R.drawable.ic_menu_report_image
) {
    ImageLoader.loadImage(this.context, this, url, placeholderRes, errorRes)
}
