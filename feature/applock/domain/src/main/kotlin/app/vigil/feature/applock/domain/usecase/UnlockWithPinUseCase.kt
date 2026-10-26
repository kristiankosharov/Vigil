package app.vigil.feature.applock.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.applock.domain.model.PinCheck

/** Correct unlocks the app; the last wrong attempt erases everything and signs out. */
public fun interface UnlockWithPinUseCase {
    public suspend operator fun invoke(pin: CharArray): AppResult<PinCheck>
}
