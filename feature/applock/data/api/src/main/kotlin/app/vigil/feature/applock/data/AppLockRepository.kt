package app.vigil.feature.applock.data

import app.vigil.common.core.AppResult
import javax.crypto.Cipher
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/**
 * The app lock. The PIN is never stored: it unwraps the vault key (`:common:security`), and a PIN is
 * right exactly when that works. Fingerprint unlock unwraps a second copy with a biometric-bound
 * Keystore key. Locking closes the vault. The session calls are in-memory and cannot fail, so they
 * return plain values. PINs are CharArrays so callers can wipe them; the repository never keeps one.
 */
public interface AppLockRepository {
    /** Locked only when a PIN exists and the session is locked. */
    public fun observeLockState(): Flow<AppResult<AppLockStateData>>

    /**
     * Wraps the vault key (the open one, or a new one) with [pin] and, when [biometric] is a cipher
     * from [prepareBiometricEnrollment] that BiometricPrompt authenticated, with the fingerprint key
     * too. Replaces any old PIN, resets the attempt count and unlocks.
     */
    public suspend fun createPin(pin: CharArray, biometric: BiometricCipherData?): AppResult<Unit>

    /** Checks [pin] by unwrapping the vault key; counts wrong attempts and erases everything after the last. */
    public suspend fun unlockWithPin(pin: CharArray): AppResult<PinCheckData>

    /** A cipher for BiometricPrompt's CryptoObject when turning fingerprint unlock on. */
    public fun prepareBiometricEnrollment(): AppResult<BiometricCipherData>

    /**
     * A cipher for BiometricPrompt's CryptoObject to unlock with; null when fingerprint unlock is off
     * or no longer valid (a fingerprint was added or removed; it is then turned off).
     */
    public suspend fun prepareBiometricUnlock(): AppResult<BiometricCipherData?>

    /** Unwraps the vault key with a cipher from [prepareBiometricUnlock] after BiometricPrompt succeeded. */
    public suspend fun unlockWithBiometric(biometric: BiometricCipherData): AppResult<Unit>

    /** Locks the session right away ("Lock wallet now"); a no-op when no PIN exists. */
    public fun lockNow()

    public fun onAppBackgrounded()

    public fun onAppForegrounded()

    /** A strong biometric is enrolled and usable on this device. */
    public fun isBiometricAvailable(): Boolean
}

/** [retryAfter] is how long PIN entry is blocked after too many wrong PINs, as of this emission. */
public data class AppLockStateData(
    val isLocked: Boolean,
    val isBiometricEnabled: Boolean,
    val retryAfter: Duration = Duration.ZERO,
)

public sealed interface PinCheckData {
    public data object Correct : PinCheckData

    /** [attemptsLeft] before everything is erased; [retryAfter] until the next PIN can be tried. */
    public data class Wrong(val attemptsLeft: Int, val retryAfter: Duration) : PinCheckData

    /** That was the last attempt: the vault and every user's data are erased and the user is signed out. */
    public data object Erased : PinCheckData
}

/** A Keystore cipher bound to a fingerprint check; only BiometricPrompt can authenticate it. */
public class BiometricCipherData(public val cipher: Cipher)
