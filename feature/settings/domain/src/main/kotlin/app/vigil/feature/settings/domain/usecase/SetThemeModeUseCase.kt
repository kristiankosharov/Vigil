package app.vigil.feature.settings.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.settings.domain.model.ThemeMode

public fun interface SetThemeModeUseCase {
    public suspend operator fun invoke(mode: ThemeMode): AppResult<Unit>
}
