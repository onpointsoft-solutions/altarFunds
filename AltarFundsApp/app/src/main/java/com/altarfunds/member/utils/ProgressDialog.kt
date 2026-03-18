package com.altarfunds.member.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.altarfunds.member.R
import com.altarfunds.member.databinding.ProgressDialogBinding

class ProgressDialog(context: Context) : Dialog(context, R.style.CustomDialog) {
    
    private val binding: ProgressDialogBinding
    
    init {
        binding = ProgressDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }
    
    fun setMessage(message: String) {
        binding.tvMessage.text = message
    }
    
    override fun show() {
        if (!isShowing) {
            super.show()
        }
    }
    
    override fun dismiss() {
        if (isShowing) {
            super.dismiss()
        }
    }
}
