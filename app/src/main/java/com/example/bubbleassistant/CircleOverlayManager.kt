package com.example.bubbleassistant

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.*
import androidx.core.content.getSystemService

object CircleOverlayManager {
    private var wm: WindowManager? = null
    private var view: CircleOverlayView? = null
    private var lp: WindowManager.LayoutParams? = null

    private fun ensureView(ctx: Context) {
        if (wm == null) wm = ctx.applicationContext.getSystemService()
        if (view != null) return

        view = CircleOverlayView(ctx.applicationContext)
        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0; y = 0
        }
        try { wm?.addView(view, lp) } catch (_: Exception) {}
    }

    fun show(ctx: Context, rect: Rect, padding: Float = 20f) {
        ensureView(ctx)
        view?.showCircle(rect, padding)
        try { wm?.updateViewLayout(view, lp) } catch (_: Exception) {}
    }

    fun hide() {
        view?.clear()
    }
}
