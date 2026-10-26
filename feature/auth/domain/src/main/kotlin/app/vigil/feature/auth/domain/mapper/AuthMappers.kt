package app.vigil.feature.auth.domain.mapper

import app.vigil.feature.auth.data.OidcProviderData
import app.vigil.feature.auth.data.PhoneVerificationData
import app.vigil.feature.auth.domain.model.OidcProvider
import app.vigil.feature.auth.domain.model.PhoneVerification

internal fun PhoneVerificationData.toDomain(): PhoneVerification = when (this) {
    is PhoneVerificationData.CodeSent -> PhoneVerification.CodeSent(verificationId)
    PhoneVerificationData.Verified -> PhoneVerification.Verified
}

internal fun OidcProviderData.toDomain() = OidcProvider(id = id, displayName = displayName)
