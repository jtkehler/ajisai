package com.jtkehler.ajisai.ocrbox

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesOcrBoxRepositoryDeviceTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearPreferences() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun profilePersistsThroughSharedPreferencesRecreation() {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val repository = SharedPreferencesOcrBoxRepository(preferences)
        repository.save(
            OcrBoxProfile(
                id = "dialogue",
                name = "Dialogue",
                normalizedRect = NormalizedRect(0.1f, 0.55f, 0.9f, 0.88f),
                priority = -1,
            ),
        )

        val restored = SharedPreferencesOcrBoxRepository(preferences).getActiveProfile()

        assertEquals("dialogue", restored.id)
        assertEquals(NormalizedRect(0.1f, 0.55f, 0.9f, 0.88f), restored.normalizedRect)
        assertTrue(OcrBoxGeometry.isValid(restored.normalizedRect))
    }

    private companion object {
        const val PREFERENCES_NAME = "ocr_box_repository_device_test"
    }
}
