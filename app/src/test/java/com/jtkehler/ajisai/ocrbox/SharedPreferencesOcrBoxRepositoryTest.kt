package com.jtkehler.ajisai.ocrbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesOcrBoxRepositoryTest {
    @Test
    fun emptyStorageReturnsDefaultProfile() {
        val repository = SharedPreferencesOcrBoxRepository(FakeStorage())

        assertEquals(listOf(OcrBoxDefaults.defaultProfile()), repository.getProfiles())
        assertEquals(OcrBoxDefaults.defaultProfile(), repository.getActiveProfile())
    }

    @Test
    fun savedProfilesSurviveRepositoryRecreationAndAreSanitized() {
        val storage = FakeStorage()
        val repository = SharedPreferencesOcrBoxRepository(storage)
        repository.save(
            OcrBoxProfile(
                id = "dialogue",
                name = "Dialogue",
                normalizedRect = NormalizedRect(-1f, 0.4f, 2f, 0.41f),
                priority = -1,
            ),
        )

        val restored = SharedPreferencesOcrBoxRepository(storage).getActiveProfile()

        assertEquals("dialogue", restored.id)
        assertTrue(OcrBoxGeometry.isValid(restored.normalizedRect))
        assertEquals(0f, restored.normalizedRect.left)
        assertEquals(1f, restored.normalizedRect.right)
    }

    @Test
    fun resetReplacesProfilesWithDefault() {
        val storage = FakeStorage()
        val repository = SharedPreferencesOcrBoxRepository(storage)
        repository.save(
            OcrBoxProfile("other", "Other", NormalizedRect(0.2f, 0.2f, 0.8f, 0.8f)),
        )

        repository.resetToDefault()

        assertEquals(listOf(OcrBoxDefaults.defaultProfile()), repository.getProfiles())
    }

    private class FakeStorage : OcrBoxProfileStorage {
        private var encoded: String? = null

        override fun read(): String? = encoded

        override fun write(encoded: String) {
            this.encoded = encoded
        }
    }
}
