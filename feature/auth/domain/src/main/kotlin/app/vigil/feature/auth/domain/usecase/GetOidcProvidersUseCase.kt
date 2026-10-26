package app.vigil.feature.auth.domain.usecase

import app.vigil.feature.auth.domain.model.OidcProvider

public fun interface GetOidcProvidersUseCase {
    public operator fun invoke(): List<OidcProvider>
}
