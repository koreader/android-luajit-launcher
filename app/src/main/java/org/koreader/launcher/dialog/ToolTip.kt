package org.koreader.launcher.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import org.koreader.launcher.R

@SuppressLint("InflateParams")
class ToolTip(context: Context) {
    private val popupWindow: PopupWindow
    private val tooltipView = LayoutInflater.from(context).inflate(R.layout.tooltip_layout, null)

    init {
        popupWindow = PopupWindow(
            tooltipView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
    }

    fun showTooltip(anchorView: View, text: String) {
        val tooltipText = tooltipView.findViewById<TextView>(R.id.tooltipText)
        tooltipText.text = text

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0], location[1] - anchorView.height)
    }

    @Suppress("unused")
    fun dismissTooltip() {
        popupWindow.dismiss()
    }

    companion object {
        fun showTooltip(anchorView: View, text: String, context: Context) {
            val tooltip = ToolTip(context)
            tooltip.showTooltip(anchorView, text)
        }
    }
}
