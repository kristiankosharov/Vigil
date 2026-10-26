package app.vigil.feature.documents.domain.usecase

/** "Lock wallet now": the app lock covers the app right away. */
public fun interface LockWalletUseCase {
    public operator fun invoke()
}
