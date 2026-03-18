package com.altarfunds.member.utils

import android.content.Context
import com.altarfunds.member.R
import java.net.UnknownHostException
import java.net.SocketTimeoutException

object ErrorHandler {
    
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "✗ No internet connection. Please check your network."
            is SocketTimeoutException -> "✗ Connection timeout. Please try again."
            is java.io.IOException -> "✗ Network error. Please check your internet connection."
            else -> "✗ Error: ${throwable.message ?: "Something went wrong"}"
        }
    }
    
    fun getHttpErrorMessage(code: Int, message: String? = null): String {
        return when (code) {
            400 -> "✗ Invalid request. Please check your input."
            401 -> "✗ Authentication failed. Please login again."
            403 -> "✗ Access denied. You don't have permission."
            404 -> "✗ Not found. The requested resource doesn't exist."
            409 -> "✗ Conflict. This resource already exists."
            422 -> "✗ Validation error. Please check all required fields."
            429 -> "✗ Too many requests. Please try again later."
            500 -> "✗ Server error. Please try again later."
            502 -> "✗ Service unavailable. Please try again later."
            503 -> "✗ Service temporarily unavailable. Please try again later."
            else -> "✗ Error: ${message ?: "Unknown error occurred"}"
        }
    }
    
    fun showError(context: Context, throwable: Throwable) {
        showToast(context, getErrorMessage(throwable))
    }
    
    fun showError(context: Context, code: Int, message: String? = null) {
        showToast(context, getHttpErrorMessage(code, message))
    }
    
    private fun showToast(context: Context, message: String) {
        if (context is android.app.Activity && !context.isFinishing) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
