package com.appletv.launcher.util

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.OvershootInterpolator

object TvFocusHelper {

    private val overshootInterpolator = OvershootInterpolator(1.4f)

    /**
     * تطبيق تأثير التركيز الخاص بـ Apple TV 4K:
     * - تكبير بنسبة 1.15x
     * - رفع منسوب الظل (Elevation)
     * - إبراز اللمعان الزجاجي العاكس
     */
    fun animateFocusState(
        cardContainer: View,
        titleView: View,
        specularHighlight: View,
        hasFocus: Boolean
    ) {
        val targetScale = if (hasFocus) 1.15f else 1.0f
        val targetElevation = if (hasFocus) 28f else 4f
        val specularAlpha = if (hasFocus) 1.0f else 0.0f
        val titleAlpha = if (hasFocus) 1.0f else 0.85f

        cardContainer.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationZ(targetElevation)
            .setDuration(220)
            .setInterpolator(overshootInterpolator)
            .start()

        specularHighlight.animate()
            .alpha(specularAlpha)
            .setDuration(200)
            .start()

        titleView.animate()
            .alpha(titleAlpha)
            .scaleX(if (hasFocus) 1.05f else 1.0f)
            .scaleY(if (hasFocus) 1.05f else 1.0f)
            .setDuration(200)
            .start()
    }

    /**
     * نبضة ناعمة وسريعة عند الضغط على التطبيق بالريموت
     */
    fun animateClickPulse(view: View, onComplete: () -> Unit) {
        view.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(70)
            .withEndAction {
                view.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(100)
                    .withEndAction {
                        onComplete()
                    }
                    .start()
            }
            .start()
    }

    /**
     * تغيير لون الإضاءة المحيطية (Ambient Backlight) بلطف
     */
    fun applyAmbientColor(glowView: View, targetColor: Int) {
        val currentColor = (glowView.background as? GradientDrawable)?.colors?.firstOrNull() ?: Color.TRANSPARENT
        
        val colorAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            currentColor,
            targetColor
        )
        colorAnimator.duration = 400
        colorAnimator.addUpdateListener { animator ->
            val animatedColor = animator.animatedValue as Int
            val drawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(animatedColor, Color.TRANSPARENT)
            ).apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 1100f
                setGradientCenter(0.5f, 0.2f)
            }
            glowView.background = drawable
        }
        colorAnimator.start()
    }
}
