package app.vigil.feature.applock.data

import app.vigil.common.core.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * The app lock: a PIN (stored as a salted hash in an encrypted DataStore), optional biometric
 * unlock, and the in-memory lock session (locked at process start and after a while in the
 * background). The session calls are in-memory and cannot fail, so they return plain values.
 */
public interface AppLockRepository {
    /** Locked only when a PIN exists and the session is locked. */
    public fun observeLockState(): Flow<AppResult<AppLockStateData>>

    /** Stores a new PIN (replacing any old one) and unlocks the session. */
    public suspend fun createPin(pin: String, biometricEnabled: Boolean): AppResult<Unit>

    /** True and unlocked when [pin] matches the stored PIN. */
    public suspend fun unlockWithPin(pin: String): AppResult<Boolean>

    /** Called after BiometricPrompt reported success. */
    public fun unlockWithBiometric()

    public fun onAppBackgrounded()

    public fun onAppForegrounded()

    /** A strong biometric is enrolled and usable on this device. */
    public fun isBiometricAvailable(): Boolean
}

public data class AppLockStateData(val isLocked: Boolean, val isBiometricEnabled: Boolean)
