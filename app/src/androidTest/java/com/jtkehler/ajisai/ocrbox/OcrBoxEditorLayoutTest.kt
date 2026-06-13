package com.jtkehler.ajisai.ocrbox

import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jtkehler.ajisai.R
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrBoxEditorLayoutTest {
    @Test
    fun editorLayoutInflatesFromServiceLikeApplicationContext() {
        val applicationContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val themedContext = ContextThemeWrapper(applicationContext, R.style.Theme_Ajisai)

        val view = LayoutInflater.from(themedContext).inflate(R.layout.overlay_ocr_box_editor, null)

        assertNotNull(view.findViewById<OcrBoxEditorView>(R.id.ocr_box_editor_view))
        assertNotNull(view.findViewById<android.view.View>(R.id.ocr_box_save_action))
        assertNotNull(view.findViewById<android.view.View>(R.id.ocr_box_cancel_action))
    }
}
