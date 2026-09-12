package com.footix.tv.ui.common

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.footix.tv.core.AppConfig

/**
 * Logo qui bat en fondu tant qu'il est visible. Sert d'indicateur de chargement
 * a la place du rond leanback : l'animation demarre et s'arrete toute seule avec
 * la visibilite de la vue, sans que l'appelant ait a la piloter.
 */
class PulsingLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private val pulse = ObjectAnimator.ofFloat(this, ALPHA, 1f, 0.25f).apply {
        duration = AppConfig.LOADING_PULSE_DURATION_MS
        repeatCount = ObjectAnimator.INFINITE
        repeatMode = ObjectAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) pulse.start() else stopPulse()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulse()
    }

    private fun stopPulse() {
        pulse.cancel()
        alpha = 1f
    }
}
