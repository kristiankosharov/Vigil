package app.vigil.feature.applock.domain.model

import javax.crypto.Cipher

/** A Keystore cipher for BiometricPrompt's CryptoObject; usable only after a fingerprint check. */
public class BiometricCipher(public val cipher: Cipher)
