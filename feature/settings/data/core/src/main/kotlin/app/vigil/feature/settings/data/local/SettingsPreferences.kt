package app.vigil.feature.settings.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The settings' encrypted store (`settings_preferences.json`). Every field has a default. */
@Serializable
internal data class SettingsPreferences(val themeMode: StoredThemeMode = StoredThemeMode.System)

/** Stored names are permanent: rename the constants freely, never the @SerialName values. */
@Serializable
internal enum class StoredThemeMode {
    @SerialName("system")
    System,

    @SerialName("light")
    Light,

    @SerialName("dark")
    Dark,
}
