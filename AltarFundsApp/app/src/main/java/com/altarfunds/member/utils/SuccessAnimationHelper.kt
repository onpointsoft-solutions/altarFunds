package com.altarfunds.member.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.altarfunds.member.R

object SuccessAnimationHelper {
    
    fun showSuccessAnimation(
        context: Context,
        containerView: View,
        onComplete: (() -> Unit)? = null
    ) {
        // Create success checkmark
        val successView = ImageView(context).apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_success_check))
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
        }
        
        // Add to container (center it)
        val layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
        
        (containerView as? android.view.ViewGroup)?.addView(successView, layoutParams)
        
        // Create animation set
        val scaleX = ObjectAnimator.ofFloat(successView, "scaleX", 0f, 1.2f, 1f).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val scaleY = ObjectAnimator.ofFloat(successView, "scaleY", 0f, 1.2f, 1f).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val alpha = ObjectAnimator.ofFloat(successView, "alpha", 0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val animatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            play(alpha).after(0)
            
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    // Keep visible for a moment then fade out
                    successView.postDelayed({
                        val fadeOut = ObjectAnimator.ofFloat(successView, "alpha", 1f, 0f).apply {
                            duration = 300
                            interpolator = AccelerateDecelerateInterpolator()
                        }
                        fadeOut.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {}
                            override fun onAnimationEnd(animation: Animator) {
                                (containerView as? android.view.ViewGroup)?.removeView(successView)
                                onComplete?.invoke()
                            }
                            override fun onAnimationCancel(animation: Animator) {}
                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                        fadeOut.start()
                    }, 1500)
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        
        animatorSet.start()
    }
    
    fun animateButtonSuccess(button: View, onComplete: (() -> Unit)? = null) {
        val originalScale = button.scaleX
        
        val scaleDown = ObjectAnimator.ofFloat(button, "scaleX", originalScale, 0.95f).apply {
            duration = 100
        }
        
        val scaleUp = ObjectAnimator.ofFloat(button, "scaleX", 0.95f, 1.05f).apply {
            duration = 100
        }
        
        val scaleNormal = ObjectAnimator.ofFloat(button, "scaleX", 1.05f, originalScale).apply {
            duration = 100
        }
        
        val scaleYDown = ObjectAnimator.ofFloat(button, "scaleY", originalScale, 0.95f).apply {
            duration = 100
        }
        
        val scaleYUp = ObjectAnimator.ofFloat(button, "scaleY", 0.95f, 1.05f).apply {
            duration = 100
        }
        
        val scaleYNormal = ObjectAnimator.ofFloat(button, "scaleY", 1.05f, originalScale).apply {
            duration = 100
        }
        
        val animatorSet = AnimatorSet().apply {
            play(scaleDown).with(scaleYDown)
            play(scaleUp).with(scaleYUp).after(scaleDown)
            play(scaleNormal).with(scaleYNormal).after(scaleUp)
            
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        
        animatorSet.start()
    }
}

// Extension function for easier usage
fun View.showSuccessAnimation(onComplete: (() -> Unit)? = null) {
    SuccessAnimationHelper.showSuccessAnimation(this.context, this, onComplete)
}

fun View.animateSuccess(onComplete: (() -> Unit)? = null) {
    SuccessAnimationHelper.animateButtonSuccess(this, onComplete)
}
