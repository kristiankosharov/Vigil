package app.vigil.feature.applock.domain.usecase

public fun interface IsBiometricAvailableUseCase {
    public operator fun invoke(): Boolean
}
