package com.jtkehler.ajisai.ocrbox

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import com.jtkehler.ajisai.R

internal interface OcrBoxEditorController {
    val isShowing: Boolean
    fun show(): Boolean
    fun dismiss(notifyClosed: Boolean = true)
}

internal class AndroidOcrBoxEditorController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val repository: OcrBoxRepository,
    private val onClosed: () -> Unit,
    private val onSaveFailed: () -> Unit,
) : OcrBoxEditorController {
    private var rootView: View? = null

    override val isShowing: Boolean
        get() = rootView != null

    override fun show(): Boolean {
        if (rootView != null) return true
        return runCatching {
            val profile = repository.getActiveProfile()
            val themedContext = ContextThemeWrapper(context, R.style.Theme_Ajisai)
            val view = LayoutInflater.from(themedContext).inflate(R.layout.overlay_ocr_box_editor, null)
            val editorView = view.findViewById<OcrBoxEditorView>(R.id.ocr_box_editor_view)
            editorView.setNormalizedRect(profile.normalizedRect)
            view.findViewById<View>(R.id.ocr_box_save_action).setOnClickListener {
                runCatching {
                    repository.save(profile.copy(normalizedRect = editorView.getNormalizedRect()))
                }.onSuccess {
                    dismiss()
                }.onFailure {
                    onSaveFailed()
                }
            }
            view.findViewById<View>(R.id.ocr_box_cancel_action).setOnClickListener {
                dismiss()
            }
            val params = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            windowManager.addView(view, params)
            rootView = view
        }.isSuccess
    }

    override fun dismiss(notifyClosed: Boolean) {
        rootView?.let { view -> runCatching { windowManager.removeView(view) } }
        val wasShowing = rootView != null
        rootView = null
        if (wasShowing && notifyClosed) onClosed()
    }
}
