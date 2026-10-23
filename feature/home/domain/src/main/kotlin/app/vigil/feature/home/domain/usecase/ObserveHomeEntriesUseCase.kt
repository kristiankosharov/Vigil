package app.vigil.feature.home.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.home.domain.model.HomeEntry
import kotlinx.coroutines.flow.Flow

public fun interface ObserveHomeEntriesUseCase {
    public operator fun invoke(): Flow<AppResult<List<HomeEntry>>>
}
