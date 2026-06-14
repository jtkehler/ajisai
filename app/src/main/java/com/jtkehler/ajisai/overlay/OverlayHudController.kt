package com.jtkehler.ajisai.overlay

import com.jtkehler.ajisai.ocr.OcrRunState
import com.jtkehler.ajisai.ocr.OcrRunner

internal interface OverlayHudSurface {
    val isShowing: Boolean
    fun show(): Boolean
    fun hide()
}

internal interface OcrHudSurface : OverlayHudSurface {
    fun render(state: OcrRunState)
}

internal class OverlayHudController(
    private val runner: OcrRunner,
    private val quickControls: OverlayHudSurface,
    private val ocrPanel: OcrHudSurface,
) {
    var isShown: Boolean = false
        private set
    private var suspendedForEditor = false

    fun toggle(): Boolean = if (isShown) {
        collapse()
        true
    } else {
        show()
    }

    fun show(): Boolean {
        if (isShown) return true
        if (!showSurfaces()) return false
        isShown = true
        suspendedForEditor = false
        runner.run()
        return true
    }

    fun collapse() {
        runner.clear()
        ocrPanel.hide()
        quickControls.hide()
        suspendedForEditor = false
        isShown = false
    }

    fun suspendForEditor() {
        if (!isShown) return
        runner.clear()
        ocrPanel.hide()
        quickControls.hide()
        suspendedForEditor = true
    }

    fun resumeAfterEditor(): Boolean {
        if (!isShown || !suspendedForEditor) return true
        if (!showSurfaces()) {
            collapse()
            return false
        }
        suspendedForEditor = false
        runner.run()
        return true
    }

    fun render(state: OcrRunState): Boolean {
        if (!isShown || suspendedForEditor) return true
        if (!ocrPanel.isShowing) return false
        ocrPanel.render(state)
        return true
    }

    private fun showSurfaces(): Boolean {
        if (!quickControls.show()) return false
        if (!ocrPanel.show()) {
            quickControls.hide()
            return false
        }
        return true
    }
}
