package com.altarfunds.mobile.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    
    private val defaultLocale = Locale.getDefault()
    
    fun formatFullDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", defaultLocale)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    fun formatShortDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", defaultLocale)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    fun formatTime(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("hh:mm a", defaultLocale)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    fun formatDateTime(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", defaultLocale)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
    
    fun isToday(dateString: String): Boolean {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = inputFormat.parse(dateString)
            val today = Calendar.getInstance()
            val inputDate = Calendar.getInstance().apply {
                time = date
            }
            
            today.get(Calendar.YEAR) == inputDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == inputDate.get(Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }
    
    fun isYesterday(dateString: String): Boolean {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = inputFormat.parse(dateString)
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val inputDate = Calendar.getInstance().apply {
                time = date
            }
            
            yesterday.get(Calendar.YEAR) == inputDate.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == inputDate.get(Calendar.DAY_OF_YEAR)
        } catch (e: Exception) {
            false
        }
    }
    
    fun getRelativeTimeString(dateString: String): String {
        return when {
            isToday(dateString) -> "Today"
            isYesterday(dateString) -> "Yesterday"
            else -> formatShortDate(dateString)
        }
    }
    
    fun getCurrentDateTime(): String {
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        return outputFormat.format(Date())
    }
}
