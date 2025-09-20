package com.example.bubbleassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Rect

class CircleOverlayReceiver : BroadcastReceiver() {
    private val regex = Regex("""@\((\d+),(\d+),(\d+)x(\d+)\)""")

    private fun parseBounds(bounds: String?): Rect? {
        if (bounds.isNullOrBlank()) return null
        val m = regex.find(bounds) ?: return null
        val (x, y, w, h) = m.groupValues.drop(1).map { it.toInt() }
        return Rect(x, y, x + w, y + h)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ChatDialogActivity.ACTION_SHOW_CIRCLE -> {
                val boundsStr = intent.getStringExtra("bounds")
                val padding = intent.getFloatExtra("padding", 20f)
                val rect = parseBounds(boundsStr)
                if (rect != null) CircleOverlayManager.show(context, rect, padding)
                else CircleOverlayManager.hide()
            }
            ChatDialogActivity.ACTION_HIDE_CIRCLE -> {
                CircleOverlayManager.hide()
            }
        }
    }
}
