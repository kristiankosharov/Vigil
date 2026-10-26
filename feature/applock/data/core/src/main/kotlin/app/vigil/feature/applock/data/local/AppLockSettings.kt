package app.vigil.feature.applock.data.local

import kotlinx.serialization.Serializable

/**
 * The app lock's encrypted store (`applock_settings.json`). Every field has a default. It never
 * holds the PIN or a hash of it: only the vault key wrapped by the PIN (and by the fingerprint key).
 */
@Serializable
internal data class AppLockSettings(
    val pinKey: PinWrappedKey? = null,
    val biometricKey: BiometricWrappedKey? = null,
    /** Wrong PINs since the last right one; counted before each check. */
    val failedAttempts: Int = 0,
    val lockout: Lockout? = null,
) {
    val hasPin: Boolean get() = pinKey != null

    // Wrapped keys must never reach a log through the generated toString().
    override fun toString(): String =
        "AppLockSettings(hasPin=$hasPin, biometric=${biometricKey != null}, failedAttempts=$failedAttempts)"
}

/** The vault key sealed with `Argon2id(PIN, salt)` + the device pepper. Base64 fields. */
@Serializable
internal data class PinWrappedKey(
    val salt: String,
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
    val sealedKey: String,
) {
    override fun toString(): String = "PinWrappedKey(***)"
}

/** The vault key encrypted by the biometric-bound Keystore key. Base64 fields. */
@Serializable
internal data class BiometricWrappedKey(val iv: String, val sealedKey: String) {
    override fun toString(): String = "BiometricWrappedKey(***)"
}

/** PIN entry is blocked until [untilElapsedMillis] (time since boot) of boot number [bootCount]. */
@Serializable
internal data class Lockout(val bootCount: Int, val untilElapsedMillis: Long)
