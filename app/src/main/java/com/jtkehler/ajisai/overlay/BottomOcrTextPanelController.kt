package com.jtkehler.ajisai.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.jtkehler.ajisai.R
import com.jtkehler.ajisai.ocr.OcrRunError
import com.jtkehler.ajisai.ocr.OcrRunState

internal class BottomOcrTextPanelController(
    private val context: Context,
    private val windowManager: WindowManager,
) : OcrHudSurface {
    private var panelView: View? = null
    private var binder: BottomOcrTextViewBinder? = null

    override val isShowing: Boolean
        get() = panelView != null

    override fun show(): Boolean {
        if (panelView != null) return true
        val margin = context.resources.getDimensionPixelSize(R.dimen.overlay_panel_margin)
        val maximumWidth = context.resources.getDimensionPixelSize(R.dimen.overlay_ocr_result_max_width)
        val bounds = context.overlayWindowBounds(windowManager)
        val width = (bounds.width - margin * 2).coerceAtMost(maximumWidth).coerceAtLeast(1)
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = margin
        }
        return runCatching {
            val view = LayoutInflater.from(context).inflate(R.layout.overlay_ocr_result, null)
            windowManager.addView(view, params)
            panelView = view
            binder = BottomOcrTextViewBinder(view).also { it.render(OcrRunState.Idle) }
        }.isSuccess
    }

    override fun render(state: OcrRunState) {
        binder?.render(state)
    }

    override fun hide() {
        panelView?.let { view -> runCatching { windowManager.removeView(view) } }
        panelView = null
        binder = null
    }
}

internal class BottomOcrTextViewBinder(root: View) {
    private val status = root.findViewById<TextView>(R.id.overlay_ocr_status)
    private val progress = root.findViewById<View>(R.id.overlay_ocr_progress)
    private val error = root.findViewById<TextView>(R.id.overlay_ocr_error)
    private val text = root.findViewById<TextView>(R.id.overlay_ocr_text)

    fun render(state: OcrRunState) {
        progress.visibility = View.GONE
        error.visibility = View.GONE
        text.visibility = View.GONE

        when (state) {
            OcrRunState.Idle -> status.setText(R.string.ocr_status_ready)
            OcrRunState.Capturing -> {
                status.setText(R.string.ocr_status_capturing)
                progress.visibility = View.VISIBLE
            }
            is OcrRunState.Recognizing -> {
                status.setText(R.string.ocr_status_recognizing)
                progress.visibility = View.VISIBLE
            }
            is OcrRunState.Success -> {
                status.setText(R.string.ocr_status_success)
                text.text = state.text
                text.visibility = View.VISIBLE
            }
            is OcrRunState.Error -> {
                status.setText(R.string.ocr_status_error)
                error.setText(state.type.messageRes())
                error.visibility = View.VISIBLE
            }
        }
    }

    private fun OcrRunError.messageRes(): Int = when (this) {
        OcrRunError.CAPTURE_INACTIVE -> R.string.ocr_error_capture_inactive
        OcrRunError.CAPTURE_UNAVAILABLE -> R.string.ocr_error_capture_unavailable
        OcrRunError.CAPTURE_TIMEOUT -> R.string.ocr_error_capture_timeout
        OcrRunError.CROP_FAILED -> R.string.ocr_error_crop_failed
        OcrRunError.PREPROCESSING -> R.string.ocr_error_preprocessing
        OcrRunError.NETWORK -> R.string.ocr_error_network
        OcrRunError.HTTP -> R.string.ocr_error_http
        OcrRunError.PARSE -> R.string.ocr_error_parse
        OcrRunError.NO_TEXT -> R.string.ocr_error_no_text
        OcrRunError.UNKNOWN -> R.string.ocr_error_unknown
    }
}
