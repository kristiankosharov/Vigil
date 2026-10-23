package app.vigil.feature.applock.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.applock.domain.model.AppLockState
import kotlinx.coroutines.flow.Flow

public fun interface ObserveAppLockStateUseCase {
    public operator fun invoke(): Flow<AppResult<AppLockState>>
}
