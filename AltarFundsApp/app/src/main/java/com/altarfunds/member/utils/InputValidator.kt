package com.altarfunds.member.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout

object InputValidator {
    
    fun addEmailValidation(editText: EditText, textInputLayout: TextInputLayout) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    textInputLayout.error = null
                    return
                }
                
                if (!s.toString().isValidEmail()) {
                    textInputLayout.error = "Invalid email format"
                    textInputLayout.isErrorEnabled = true
                } else {
                    textInputLayout.error = null
                    textInputLayout.isErrorEnabled = false
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    fun addPhoneValidation(editText: EditText, textInputLayout: TextInputLayout) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    textInputLayout.error = null
                    return
                }
                
                val formatted = s.toString().formatPhoneNumber()
                if (formatted != s.toString()) {
                    editText.removeTextChangedListener(this)
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                    editText.addTextChangedListener(this)
                }
                
                if (!formatted.isValidPhone()) {
                    textInputLayout.error = "Invalid phone number (254XXXXXXXXX or 07XXXXXXXXX)"
                    textInputLayout.isErrorEnabled = true
                } else {
                    textInputLayout.error = null
                    textInputLayout.isErrorEnabled = false
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    fun addPasswordValidation(editText: EditText, textInputLayout: TextInputLayout, 
                            confirmEditText: EditText? = null, 
                            confirmTextInputLayout: TextInputLayout? = null) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    textInputLayout.error = null
                    return
                }
                
                when {
                    s.length < 8 -> {
                        textInputLayout.error = "Password must be at least 8 characters"
                        textInputLayout.isErrorEnabled = true
                    }
                    !s.toString().contains(Regex("[A-Z]")) -> {
                        textInputLayout.error = "Include at least one uppercase letter"
                        textInputLayout.isErrorEnabled = true
                    }
                    !s.toString().contains(Regex("[a-z]")) -> {
                        textInputLayout.error = "Include at least one lowercase letter"
                        textInputLayout.isErrorEnabled = true
                    }
                    !s.toString().contains(Regex("[0-9]")) -> {
                        textInputLayout.error = "Include at least one number"
                        textInputLayout.isErrorEnabled = true
                    }
                    else -> {
                        textInputLayout.error = null
                        textInputLayout.isErrorEnabled = false
                    }
                }
                
                // Check confirm password if provided
                if (confirmEditText != null && confirmTextInputLayout != null) {
                    val confirmText = confirmEditText.text.toString()
                    if (confirmText.isNotEmpty() && confirmText != s.toString()) {
                        confirmTextInputLayout.error = "Passwords do not match"
                        confirmTextInputLayout.isErrorEnabled = true
                    } else if (confirmText.isNotEmpty()) {
                        confirmTextInputLayout.error = null
                        confirmTextInputLayout.isErrorEnabled = false
                    }
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    fun addRequiredFieldValidation(editText: EditText, textInputLayout: TextInputLayout, fieldName: String) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    textInputLayout.error = "$fieldName is required"
                    textInputLayout.isErrorEnabled = true
                } else {
                    textInputLayout.error = null
                    textInputLayout.isErrorEnabled = false
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    fun validateForm(vararg pairs: Pair<EditText, TextInputLayout>): Boolean {
        var isValid = true
        
        for ((editText, textInputLayout) in pairs) {
            if (editText.text.isNullOrEmpty()) {
                textInputLayout.error = "This field is required"
                textInputLayout.isErrorEnabled = true
                isValid = false
            } else if (textInputLayout.isErrorEnabled) {
                isValid = false
            }
        }
        
        return isValid
    }
}
