package app.vigil.feature.applock.data.local

import kotlinx.serialization.Serializable

/** The app lock's encrypted store (`applock_settings.json`). Every field has a default. */
@Serializable
internal data class AppLockSettings(
    val pinHash: String? = null, // Base64 PBKDF2 hash, never the PIN itself
    val pinSalt: String? = null, // Base64
    val pinIterations: Int = 0,
    val biometricEnabled: Boolean = false,
) {
    val hasPin: Boolean get() = pinHash != null && pinSalt != null

    // Hash and salt must never reach a log through the generated toString().
    override fun toString(): String = "AppLockSettings(hasPin=$hasPin, biometricEnabled=$biometricEnabled)"
}
