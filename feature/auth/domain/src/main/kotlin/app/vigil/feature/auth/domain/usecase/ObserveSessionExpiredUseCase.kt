package app.vigil.feature.auth.domain.usecase

import kotlinx.coroutines.flow.Flow

public fun interface ObserveSessionExpiredUseCase {
    public operator fun invoke(): Flow<Unit>
}
