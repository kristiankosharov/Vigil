package app.vigil.feature.applock.domain.usecase

import app.vigil.common.core.AppResult

/** Success(true) when the PIN matched and the app is now unlocked. */
public fun interface UnlockWithPinUseCase {
    public suspend operator fun invoke(pin: String): AppResult<Boolean>
}
