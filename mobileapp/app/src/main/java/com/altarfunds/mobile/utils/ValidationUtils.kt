package com.altarfunds.mobile.utils

import android.util.Patterns

object ValidationUtils {
    
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidPhoneNumber(phone: String): Boolean {
        // Kenyan phone number validation (starts with 07 or 01 and has 10 digits)
        val kenyanPhoneRegex = Regex("^(07|01)[0-9]{8}$")
        return kenyanPhoneRegex.matches(phone.replace(Regex("[^0-9]"), ""))
    }
    
    fun isValidAmount(amount: String): Boolean {
        return try {
            val cleanAmount = amount.replace(Regex("[^0-9.]"), "")
            val value = cleanAmount.toDouble()
            value > 0 && value <= 1000000 // 1 million KES limit
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    fun isValidName(name: String): Boolean {
        return name.isNotBlank() && name.length >= 2 && name.length <= 100
    }
    
    fun isValidPassword(password: String): Boolean {
        // At least 8 characters, 1 uppercase, 1 lowercase, 1 number
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$")
        return passwordRegex.matches(password)
    }
    
    fun formatPhoneNumber(phone: String): String {
        // Format to Kenyan standard: 07XX XXX XXX
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        return if (cleanPhone.length == 10) {
            "${cleanPhone.substring(0, 4)} ${cleanPhone.substring(4, 7)} ${cleanPhone.substring(7)}"
        } else {
            phone
        }
    }
    
    fun sanitizeInput(input: String): String {
        return input.trim().replace(Regex("[<>\"'&]"), "")
    }
    
    fun isValidMembershipNumber(membershipNumber: String): Boolean {
        // Membership number validation (alphanumeric, 6-20 characters)
        val membershipRegex = Regex("^[A-Za-z0-9]{6,20}$")
        return membershipRegex.matches(membershipNumber)
    }
    
    fun isValidText(text: String, minLength: Int = 1, maxLength: Int = 500): Boolean {
        return text.isNotBlank() && 
               text.length >= minLength && 
               text.length <= maxLength &&
               !text.contains(Regex("[<>\"'&]"))
    }
    
    fun validateGivingAmount(amount: String): ValidationResult {
        return when {
            amount.isEmpty() -> ValidationResult(false, "Amount is required")
            !isValidAmount(amount) -> ValidationResult(false, "Please enter a valid amount")
            amount.toDouble() > 1000000 -> ValidationResult(false, "Amount exceeds maximum limit")
            amount.toDouble() < 10 -> ValidationResult(false, "Minimum amount is KES 10")
            else -> ValidationResult(true, "Valid amount")
        }
    }
    
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult(false, "Email is required")
            !isValidEmail(email) -> ValidationResult(false, "Please enter a valid email address")
            else -> ValidationResult(true, "Valid email")
        }
    }
    
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(false, "Password is required")
            password.length < 8 -> ValidationResult(false, "Password must be at least 8 characters")
            !isValidPassword(password) -> ValidationResult(false, "Password must contain uppercase, lowercase, and number")
            else -> ValidationResult(true, "Valid password")
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String
)
