package app.vigil.feature.settings.data

import app.vigil.common.core.AppResult
import kotlinx.coroutines.flow.Flow

/** App settings that belong to the device, not to the signed-in user (kept on logout). */
public interface SettingsRepository {
    /** The chosen theme; [ThemeModeData.System] until the user picks one. */
    public fun observeThemeMode(): Flow<AppResult<ThemeModeData>>

    public suspend fun setThemeMode(mode: ThemeModeData): AppResult<Unit>
}

public enum class ThemeModeData { System, Light, Dark }
