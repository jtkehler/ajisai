package com.jtkehler.ajisai.ocrbox

import android.content.SharedPreferences
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

interface OcrBoxRepository {
    fun getProfiles(): List<OcrBoxProfile>
    fun getActiveProfile(): OcrBoxProfile
    fun save(profile: OcrBoxProfile)
    fun resetToDefault(): OcrBoxProfile
}

internal interface OcrBoxProfileStorage {
    fun read(): String?
    fun write(encoded: String)
}

class SharedPreferencesOcrBoxRepository private constructor(
    private val storage: OcrBoxProfileStorage,
) : OcrBoxRepository {
    constructor(preferences: SharedPreferences) : this(SharedPreferencesStorage(preferences))

    internal constructor(storage: OcrBoxProfileStorage, testOnly: Unit = Unit) : this(storage)

    private val lock = Any()

    override fun getProfiles(): List<OcrBoxProfile> = synchronized(lock) {
        readProfiles()
    }

    override fun getActiveProfile(): OcrBoxProfile = synchronized(lock) {
        val profiles = readProfiles()
        profiles.filter(OcrBoxProfile::enabled).minByOrNull(OcrBoxProfile::priority)
            ?: profiles.minByOrNull(OcrBoxProfile::priority)
            ?: OcrBoxDefaults.defaultProfile()
    }

    override fun save(profile: OcrBoxProfile) = synchronized(lock) {
        val sanitized = profile.copy(
            id = profile.id.ifBlank { OcrBoxDefaults.DEFAULT_ID },
            name = profile.name.ifBlank { "OCR box" },
            normalizedRect = OcrBoxGeometry.sanitize(profile.normalizedRect),
        )
        val profiles = readProfiles().toMutableList()
        val existingIndex = profiles.indexOfFirst { it.id == sanitized.id }
        if (existingIndex >= 0) {
            profiles[existingIndex] = sanitized
        } else {
            profiles += sanitized
        }
        storage.write(OcrBoxProfileCodec.encode(profiles))
    }

    override fun resetToDefault(): OcrBoxProfile = synchronized(lock) {
        val profile = OcrBoxDefaults.defaultProfile()
        storage.write(OcrBoxProfileCodec.encode(listOf(profile)))
        profile
    }

    private fun readProfiles(): List<OcrBoxProfile> {
        val profiles = storage.read()
            ?.let(OcrBoxProfileCodec::decode)
            .orEmpty()
            .distinctBy(OcrBoxProfile::id)
            .sortedWith(compareBy(OcrBoxProfile::priority, OcrBoxProfile::name))
        return profiles.ifEmpty { listOf(OcrBoxDefaults.defaultProfile()) }
    }

    private class SharedPreferencesStorage(
        private val preferences: SharedPreferences,
    ) : OcrBoxProfileStorage {
        override fun read(): String? = preferences.getString(KEY_PROFILES, null)

        override fun write(encoded: String) {
            preferences.edit().putString(KEY_PROFILES, encoded).apply()
        }
    }

    private companion object {
        const val KEY_PROFILES = "ocr_box_profiles"
    }
}

internal object OcrBoxProfileCodec {
    fun encode(profiles: List<OcrBoxProfile>): String {
        val properties = Properties()
        properties.setProperty("count", profiles.size.toString())
        profiles.forEachIndexed { index, profile ->
            val prefix = "profile.$index."
            properties.setProperty(prefix + "id", profile.id)
            properties.setProperty(prefix + "name", profile.name)
            properties.setProperty(prefix + "left", profile.normalizedRect.left.toString())
            properties.setProperty(prefix + "top", profile.normalizedRect.top.toString())
            properties.setProperty(prefix + "right", profile.normalizedRect.right.toString())
            properties.setProperty(prefix + "bottom", profile.normalizedRect.bottom.toString())
            properties.setProperty(prefix + "enabled", profile.enabled.toString())
            properties.setProperty(prefix + "priority", profile.priority.toString())
            profile.lastUsedAtMs?.let { properties.setProperty(prefix + "lastUsedAtMs", it.toString()) }
        }
        return StringWriter().also { properties.store(it, null) }.toString()
    }

    fun decode(encoded: String): List<OcrBoxProfile> = runCatching {
        val properties = Properties().apply { load(StringReader(encoded)) }
        val count = properties.getProperty("count")?.toIntOrNull()?.coerceIn(0, MAX_PROFILES) ?: 0
        buildList {
            repeat(count) { index ->
                val prefix = "profile.$index."
                val id = properties.getProperty(prefix + "id").orEmpty()
                if (id.isBlank()) return@repeat
                val rect = NormalizedRect(
                    left = properties.float(prefix + "left", OcrBoxDefaults.defaultRect.left),
                    top = properties.float(prefix + "top", OcrBoxDefaults.defaultRect.top),
                    right = properties.float(prefix + "right", OcrBoxDefaults.defaultRect.right),
                    bottom = properties.float(prefix + "bottom", OcrBoxDefaults.defaultRect.bottom),
                )
                add(
                    OcrBoxProfile(
                        id = id,
                        name = properties.getProperty(prefix + "name").orEmpty().ifBlank { "OCR box" },
                        normalizedRect = OcrBoxGeometry.sanitize(rect),
                        enabled = properties.getProperty(prefix + "enabled")?.toBooleanStrictOrNull() ?: true,
                        priority = properties.getProperty(prefix + "priority")?.toIntOrNull() ?: 0,
                        lastUsedAtMs = properties.getProperty(prefix + "lastUsedAtMs")?.toLongOrNull(),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun Properties.float(key: String, fallback: Float): Float =
        getProperty(key)?.toFloatOrNull()?.takeIf(Float::isFinite) ?: fallback

    private const val MAX_PROFILES = 100
}
