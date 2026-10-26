package app.vigil.feature.applock.ui.biometric

import app.vigil.feature.applock.domain.model.BiometricCipher

/**
 * A consumable "show the biometric prompt now" request with the Keystore cipher the prompt
 * authenticates. A plain class: identity, like effects.
 */
internal class BiometricRequest(val cipher: BiometricCipher)
