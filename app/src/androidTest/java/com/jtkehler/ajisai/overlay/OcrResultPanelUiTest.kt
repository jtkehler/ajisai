package com.jtkehler.ajisai.overlay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jtkehler.ajisai.MainActivity
import com.jtkehler.ajisai.R
import com.jtkehler.ajisai.ocr.OcrRunError
import com.jtkehler.ajisai.ocr.OcrRunState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrResultPanelUiTest {
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var binder: BottomOcrTextViewBinder

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun displaysLoadingReadOnlySuccessAndCompactErrorWithoutRetry() {
        scenario = ActivityScenario.launch(MainActivity::class.java).also { launched ->
            launched.onActivity { activity ->
                val root = LayoutInflater.from(activity).inflate(R.layout.overlay_ocr_result, null)
                activity.addContentView(
                    root,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
                binder = BottomOcrTextViewBinder(root)
                val text = root.findViewById<TextView>(R.id.overlay_ocr_text)
                assertFalse(text is EditText)
                assertFalse(text.isFocusable)
                assertFalse(root.containsEditText())
                binder.render(OcrRunState.Capturing)
            }
        }

        onView(withId(R.id.overlay_ocr_progress)).check(matches(isDisplayed()))
        onView(withId(R.id.overlay_ocr_status)).check(matches(withText(R.string.ocr_status_capturing)))

        scenario?.onActivity {
            binder.render(OcrRunState.Success("元の文", 123L, "Fake"))
        }
        onView(withId(R.id.overlay_ocr_text))
            .check(matches(isDisplayed()))
            .check(matches(withText("元の文")))
        onView(withText(R.string.ocr_retry)).check(doesNotExist())

        scenario?.onActivity {
            binder.render(OcrRunState.Error(OcrRunError.NETWORK))
        }
        onView(withId(R.id.overlay_ocr_error))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.ocr_error_network)))
    }

    private fun View.containsEditText(): Boolean {
        if (this is EditText) return true
        if (this !is ViewGroup) return false
        return (0 until childCount).any { getChildAt(it).containsEditText() }
    }
}
