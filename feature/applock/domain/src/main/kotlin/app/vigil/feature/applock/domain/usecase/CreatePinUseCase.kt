package app.vigil.feature.applock.domain.usecase

import app.vigil.common.core.AppResult

/** Callers check [app.vigil.feature.applock.domain.model.PinRules] first. */
public fun interface CreatePinUseCase {
    public suspend operator fun invoke(pin: String, biometricEnabled: Boolean): AppResult<Unit>
}
