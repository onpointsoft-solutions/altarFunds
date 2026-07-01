package com.sanctum.member.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sanctum.member.models.ChurchThemeColors
import com.sanctum.member.R

/**
 * Global theme manager — applies church branding to:
 *  - App top bar (stays app primary purple — only text/logo changes)
 *  - Bottom nav (church primary colour for selected, muted for unselected)
 *  - Church logo in top bar
 *  - Church name in top bar
 */
object ThemeManager {

    private var currentTheme: ChurchThemeColors? = null

    // ── Public entry point ────────────────────────────────────────────────

    fun applyChurchTheme(context: Context, theme: ChurchThemeColors?) {
        currentTheme = theme
        if (context !is AppCompatActivity) return
        try {
            applyBottomNav(context, theme)
            applyTopBar(context, theme)
        } catch (_: Exception) { /* never crash on theme errors */ }
    }

    // ── Top bar ───────────────────────────────────────────────────────────
    // Top bar stays the app brand colour (#260E68). Only the church logo
    // and church name are swapped in.

    private fun applyTopBar(activity: AppCompatActivity, theme: ChurchThemeColors?) {
        val toolbar = activity.findViewById<Toolbar>(R.id.topBar) ?: return

        // Church name
        val tvName = toolbar.findViewById<TextView>(R.id.tvChurchName)
        tvName?.text = theme?.church_name?.ifBlank { "AltarFunds" } ?: "AltarFunds"

        // Church logo — load with Glide, fall back to default launcher icon
        val imgLogo = toolbar.findViewById<ImageView>(R.id.imgChurchLogo)
        imgLogo?.let { iv ->
            val logoUrl = theme?.logo_url
            if (!logoUrl.isNullOrBlank()) {
                Glide.with(activity)
                    .load(logoUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .circleCrop()
                    .into(iv)
            } else {
                iv.setImageResource(R.mipmap.ic_launcher)
            }
        }
    }

    // ── Bottom nav ────────────────────────────────────────────────────────
    // Background  → white (church-agnostic, clean look)
    // Selected    → church primary colour
    // Unselected  → 40 % opacity version of church primary (grey-ish feel)
    // Indicator   → church primary at 12 % alpha (Material 3 pill)

    private fun applyBottomNav(activity: AppCompatActivity, theme: ChurchThemeColors?) {
        val nav = activity.findViewById<BottomNavigationView>(R.id.bottomNav) ?: return

        val primaryHex  = theme?.primary_color  ?: "#260E68"
        val primary     = parseColorSafe(primaryHex, ContextCompat.getColor(activity, R.color.primary))
        val inactive    = Color.argb(120, Color.red(primary), Color.green(primary), Color.blue(primary))
        val indicator   = Color.argb(30,  Color.red(primary), Color.green(primary), Color.blue(primary))

        val states = arrayOf(
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf( android.R.attr.state_checked)
        )
        val iconColors = intArrayOf(inactive, primary)
        val csl = ColorStateList(states, iconColors)

        nav.itemIconTintList  = csl
        nav.itemTextColor     = csl
        nav.itemActiveIndicatorColor = ColorStateList.valueOf(indicator)
        nav.setBackgroundColor(Color.WHITE)
    }

    // ── Colour helpers ────────────────────────────────────────────────────

    private fun parseColorSafe(hex: String, fallback: Int): Int = try {
        Color.parseColor(hex)
    } catch (_: Exception) { fallback }

    fun getPrimaryColor(context: Context): Int =
        parseColorSafe(
            currentTheme?.primary_color ?: "",
            ContextCompat.getColor(context, R.color.primary)
        )

    fun getSecondaryColor(context: Context): Int =
        parseColorSafe(
            currentTheme?.secondary_color ?: "",
            ContextCompat.getColor(context, R.color.secondary)
        )

    fun getAccentColor(context: Context): Int =
        parseColorSafe(
            currentTheme?.accent_color ?: "",
            ContextCompat.getColor(context, R.color.accent)
        )

    fun getCurrentTheme(): ChurchThemeColors? = currentTheme

    fun resetTheme() { currentTheme = null }

    /**
     * Apply a single theme colour to a view (primary / secondary / accent).
     */
    fun applyThemeToView(view: android.view.View, colorType: String = "primary") {
        val color = when (colorType.lowercase()) {
            "primary"   -> currentTheme?.let { parseColorSafe(it.primary_color,   0) }
            "secondary" -> currentTheme?.let { parseColorSafe(it.secondary_color, 0) }
            "accent"    -> currentTheme?.let { parseColorSafe(it.accent_color,    0) }
            else        -> null
        } ?: return

        when (view) {
            is com.google.android.material.button.MaterialButton -> {
                view.setBackgroundColor(color); view.setTextColor(Color.WHITE)
            }
            is android.widget.Button   -> { view.setBackgroundColor(color); view.setTextColor(Color.WHITE) }
            is android.widget.TextView -> view.setTextColor(color)
            else                       -> view.setBackgroundColor(color)
        }
    }
}
