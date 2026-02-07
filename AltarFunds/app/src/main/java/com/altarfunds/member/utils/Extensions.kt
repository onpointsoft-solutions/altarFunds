package com.altarfunds.member.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun String.formatCurrency(): String {
    return try {
        val amount = this.toDoubleOrNull() ?: 0.0
        val format = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
        format.format(amount)
    } catch (e: Exception) {
        "KES 0.00"
    }
}

fun String.formatDate(inputFormat: String = "yyyy-MM-dd'T'HH:mm:ss", outputFormat: String = "MMM dd, yyyy"): String {
    return try {
        val parser = SimpleDateFormat(inputFormat, Locale.getDefault())
        val formatter = SimpleDateFormat(outputFormat, Locale.getDefault())
        val date = parser.parse(this)
        date?.let { formatter.format(it) } ?: this
    } catch (e: Exception) {
        this
    }
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPhone(): Boolean {
    val phonePattern = "^(\\+254|0)[17]\\d{8}$".toRegex()
    return phonePattern.matches(this)
}

fun String.formatPhoneNumber(): String {
    // Convert 0712345678 to 254712345678
    return when {
        this.startsWith("0") -> "254${this.substring(1)}"
        this.startsWith("+254") -> this.substring(1)
        this.startsWith("254") -> this
        else -> this
    }
}
