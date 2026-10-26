package app.vigil.feature.auth.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.auth.domain.model.PhoneVerification
import kotlinx.coroutines.flow.Flow

/** Sends an SMS code; emits `CodeSent`, then `Verified` if the device verifies by itself. */
public fun interface RequestPhoneCodeUseCase {
    public operator fun invoke(phoneNumber: String): Flow<AppResult<PhoneVerification>>
}
