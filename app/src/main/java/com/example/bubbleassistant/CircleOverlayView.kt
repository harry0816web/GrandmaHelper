package com.example.bubbleassistant

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#81D8D0") // Tiffany 綠
        alpha = 100                         // 可以調整透明度 (0~255)
    }

    private var circleCenter: PointF? = null
    private var circleRadius: Float = 0f

    private var animator: ValueAnimator? = null

    fun showCircle(targetRect: Rect, padding: Float = 20f, yOffset: Float = -20f) {
        circleCenter = PointF(
            targetRect.exactCenterX(),
            targetRect.exactCenterY() + yOffset * 4   // 可調整往上移
        )
        circleRadius = (
                (targetRect.width().coerceAtLeast(targetRect.height()) / 2f) + padding
                ).coerceIn(100f, 200f)

        startBlinking()
        invalidate()
    }

    fun clear() {
        circleCenter = null
        stopBlinking()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        circleCenter?.let { center ->
            canvas.drawCircle(center.x, center.y, circleRadius, paint)
        }
    }

    // --- 閃爍動畫 ---
    private fun startBlinking() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofInt(0, 200).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                paint.alpha = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun stopBlinking() {
        animator?.cancel()
        animator = null
    }
}
