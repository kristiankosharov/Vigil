package app.vigil.feature.applock.domain.model

import kotlin.time.Duration

/** [retryAfter]: how long PIN entry is blocked after too many wrong PINs, as of this state. */
public data class AppLockState(
    val isLocked: Boolean,
    val isBiometricEnabled: Boolean,
    val retryAfter: Duration = Duration.ZERO,
)
