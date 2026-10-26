package app.vigil.feature.settings.domain.mapper

import app.vigil.feature.settings.data.ThemeModeData
import app.vigil.feature.settings.domain.model.ThemeMode

internal fun ThemeModeData.toDomain(): ThemeMode = when (this) {
    ThemeModeData.System -> ThemeMode.System
    ThemeModeData.Light -> ThemeMode.Light
    ThemeModeData.Dark -> ThemeMode.Dark
}

internal fun ThemeMode.toData(): ThemeModeData = when (this) {
    ThemeMode.System -> ThemeModeData.System
    ThemeMode.Light -> ThemeModeData.Light
    ThemeMode.Dark -> ThemeModeData.Dark
}
