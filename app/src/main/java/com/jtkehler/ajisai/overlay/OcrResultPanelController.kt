package com.jtkehler.ajisai.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.jtkehler.ajisai.R
import com.jtkehler.ajisai.ocr.OcrRunError
import com.jtkehler.ajisai.ocr.OcrRunState

internal class OcrResultPanelController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onRetry: () -> Unit,
    private val onTextChanged: (String) -> Unit,
    private val onClearClose: () -> Unit,
) {
    private var panelView: View? = null
    private var binder: OcrResultViewBinder? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var bubblePosition = BubblePosition(0, 0)
    private var bubbleSize = 0

    val isShowing: Boolean
        get() = panelView != null

    fun render(state: OcrRunState): Boolean {
        if (state == OcrRunState.Idle) {
            hide()
            return true
        }
        if (!show()) return false
        binder?.render(state)
        return true
    }

    fun hide() {
        panelView?.let { view -> runCatching { windowManager.removeView(view) } }
        panelView = null
        binder = null
        layoutParams = null
    }

    fun updateAnchor(position: BubblePosition, size: Int) {
        bubblePosition = position
        bubbleSize = size
        val view = panelView ?: return
        val params = layoutParams ?: return
        updatePanelPosition(params, params.width)
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun show(): Boolean {
        if (panelView != null) return true
        val panelWidth = context.resources.getDimensionPixelSize(R.dimen.overlay_ocr_result_width)
        val params = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        updatePanelPosition(params, panelWidth)
        return runCatching {
            val view = LayoutInflater.from(context).inflate(R.layout.overlay_ocr_result, null)
            val viewBinder = OcrResultViewBinder(
                root = view,
                onRetry = onRetry,
                onTextChanged = onTextChanged,
                onCopy = ::copyText,
                onClearClose = onClearClose,
            )
            windowManager.addView(view, params)
            panelView = view
            binder = viewBinder
            layoutParams = params
        }.isSuccess
    }

    private fun copyText(text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.app_name), text))
        Toast.makeText(context, R.string.ocr_copied, Toast.LENGTH_SHORT).show()
    }

    private fun updatePanelPosition(params: WindowManager.LayoutParams, panelWidth: Int) {
        val margin = context.resources.getDimensionPixelSize(R.dimen.overlay_panel_margin)
        val panelHeight = context.resources.getDimensionPixelSize(
            R.dimen.overlay_ocr_result_estimated_height,
        )
        val bounds = context.overlayWindowBounds(windowManager)
        val leftOfBubble = bubblePosition.x - panelWidth - margin
        params.x = if (leftOfBubble >= 0) {
            leftOfBubble
        } else {
            (bubblePosition.x + bubbleSize + margin)
                .coerceAtMost((bounds.width - panelWidth).coerceAtLeast(0))
        }
        params.y = bubblePosition.y.coerceIn(
            0,
            (bounds.height - panelHeight).coerceAtLeast(0),
        )
    }
}

internal class OcrResultViewBinder(
    root: View,
    onRetry: () -> Unit,
    private val onTextChanged: (String) -> Unit,
    onCopy: (String) -> Unit,
    onClearClose: () -> Unit,
) {
    private val status = root.findViewById<TextView>(R.id.overlay_ocr_status)
    private val progress = root.findViewById<View>(R.id.overlay_ocr_progress)
    private val error = root.findViewById<TextView>(R.id.overlay_ocr_error)
    private val text = root.findViewById<EditText>(R.id.overlay_ocr_text)
    private val retry = root.findViewById<View>(R.id.overlay_ocr_retry)
    private val copy = root.findViewById<View>(R.id.overlay_ocr_copy)
    private var updatingText = false

    init {
        retry.setOnClickListener { onRetry() }
        copy.setOnClickListener { onCopy(text.text.toString()) }
        root.findViewById<View>(R.id.overlay_ocr_clear_close).setOnClickListener {
            onClearClose()
        }
        text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(value: Editable?) {
                if (!updatingText) onTextChanged(value?.toString().orEmpty())
            }
        })
    }

    fun render(state: OcrRunState) {
        progress.visibility = View.GONE
        error.visibility = View.GONE
        text.visibility = View.GONE
        retry.visibility = View.GONE
        copy.visibility = View.GONE

        when (state) {
            OcrRunState.Idle -> Unit
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
                text.visibility = View.VISIBLE
                retry.visibility = View.VISIBLE
                copy.visibility = View.VISIBLE
                if (text.text.toString() != state.text) {
                    updatingText = true
                    text.setText(state.text)
                    text.setSelection(text.text.length)
                    updatingText = false
                }
            }
            is OcrRunState.Error -> {
                status.setText(R.string.ocr_error_unknown)
                error.setText(state.type.messageRes())
                error.visibility = View.VISIBLE
                retry.visibility = View.VISIBLE
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
