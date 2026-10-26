package app.vigil.feature.settings.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

public fun interface ObserveThemeModeUseCase {
    public operator fun invoke(): Flow<AppResult<ThemeMode>>
}
