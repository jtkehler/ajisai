package com.jtkehler.ajisai.settings

/** Minimal settings snapshot; later stages can add typed feature settings. */
data class AppSettings(
    val darkThemeEnabled: Boolean = true,
)

/** Boundary for settings persistence without choosing a storage implementation yet. */
interface SettingsRepository {
    suspend fun load(): AppSettings

    suspend fun save(settings: AppSettings)
}
