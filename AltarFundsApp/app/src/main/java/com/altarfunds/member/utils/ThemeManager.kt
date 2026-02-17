package com.altarfunds.member.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.altarfunds.member.models.ChurchThemeColors
import com.altarfunds.member.R

/**
 * Global theme manager for applying church branding across the app
 */
object ThemeManager {
    
    private var currentTheme: ChurchThemeColors? = null
    
    /**
     * Apply church theme to the entire app
     */
    fun applyChurchTheme(context: Context, theme: ChurchThemeColors) {
        currentTheme = theme
        
        try {
            // Apply theme to activity if it's AppCompatActivity
            if (context is AppCompatActivity) {
                applyActivityTheme(context, theme)
            }
            
            // Apply theme to bottom navigation if available
            applyBottomNavigationTheme(context, theme)
            
        } catch (e: Exception) {
            // If theme application fails, continue with default theme
        }
    }
    
    /**
     * Apply theme to activity components
     */
    private fun applyActivityTheme(activity: AppCompatActivity, theme: ChurchThemeColors) {
        try {
            val primaryColor = Color.parseColor(theme.primary_color)
            val secondaryColor = Color.parseColor(theme.secondary_color)
            val accentColor = Color.parseColor(theme.accent_color)
            
            // Update action bar
            activity.supportActionBar?.let { actionBar ->
                actionBar.setBackgroundDrawable(ColorDrawable(primaryColor))
                
                // Set status bar color
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    activity.window.statusBarColor = primaryColor
                }
            }
            
            // Update navigation bar color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                activity.window.navigationBarColor = accentColor
            }
            
        } catch (e: Exception) {
            // Continue with default colors if parsing fails
        }
    }
    
    /**
     * Apply theme to bottom navigation
     */
    private fun applyBottomNavigationTheme(context: Context, theme: ChurchThemeColors) {
        try {
            val primaryColor = Color.parseColor(theme.primary_color)
            val secondaryColor = Color.parseColor(theme.secondary_color)
            
            // Find bottom navigation in the current activity
            val bottomNav = (context as? AppCompatActivity)?.findViewById<BottomNavigationView>(
                R.id.bottomNav
            )
            
            bottomNav?.let { navView ->
                // Create color state list for navigation items
                val colors = intArrayOf(secondaryColor, primaryColor)
                val states = arrayOf(
                    intArrayOf(-android.R.attr.state_checked),
                    intArrayOf(android.R.attr.state_checked)
                )
                
                val colorStateList = ColorStateList(states, colors)
                navView.itemTextColor = colorStateList
                navView.itemIconTintList = colorStateList
                
                // Apply background color
                navView.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface)
                )
            }
            
        } catch (e: Exception) {
            // Continue with default bottom navigation theme
        }
    }
    
    /**
     * Get the current theme colors
     */
    fun getCurrentTheme(): ChurchThemeColors? = currentTheme
    
    /**
     * Get primary color with fallback
     */
    fun getPrimaryColor(context: Context): Int {
        return try {
            currentTheme?.let { Color.parseColor(it.primary_color) }
                ?: ContextCompat.getColor(context, R.color.primary)
        } catch (e: Exception) {
            ContextCompat.getColor(context, R.color.primary)
        }
    }
    
    /**
     * Get secondary color with fallback
     */
    fun getSecondaryColor(context: Context): Int {
        return try {
            currentTheme?.let { Color.parseColor(it.secondary_color) }
                ?: ContextCompat.getColor(context, R.color.secondary)
        } catch (e: Exception) {
            ContextCompat.getColor(context, R.color.secondary)
        }
    }
    
    /**
     * Get accent color with fallback
     */
    fun getAccentColor(context: Context): Int {
        return try {
            currentTheme?.let { Color.parseColor(it.accent_color) }
                ?: ContextCompat.getColor(context, R.color.accent)
        } catch (e: Exception) {
            ContextCompat.getColor(context, R.color.accent)
        }
    }
    
    /**
     * Apply theme to a specific view
     */
    fun applyThemeToView(view: android.view.View, colorType: String = "primary") {
        try {
            val color = when (colorType.lowercase()) {
                "primary" -> currentTheme?.let { Color.parseColor(it.primary_color) }
                "secondary" -> currentTheme?.let { Color.parseColor(it.secondary_color) }
                "accent" -> currentTheme?.let { Color.parseColor(it.accent_color) }
                else -> null
            }
            
            color?.let {
                when (view) {
                    is com.google.android.material.button.MaterialButton -> {
                        view.setBackgroundColor(it)
                        view.setTextColor(Color.WHITE)
                    }
                    is android.widget.Button -> {
                        view.setBackgroundColor(it)
                        view.setTextColor(Color.WHITE)
                    }
                    is android.widget.TextView -> {
                        view.setTextColor(it)
                    }
                    else -> {
                        // For other view types, try to set background
                        view.setBackgroundColor(it)
                    }
                }
            }
        } catch (e: Exception) {
            // Continue with default styling
        }
    }
    
    /**
     * Reset theme to default
     */
    fun resetTheme() {
        currentTheme = null
    }
}
