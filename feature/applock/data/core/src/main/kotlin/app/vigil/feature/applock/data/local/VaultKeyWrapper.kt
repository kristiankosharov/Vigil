package app.vigil.feature.applock.data.local

import app.vigil.common.security.AesGcm
import app.vigil.common.security.KdfParams
import app.vigil.common.security.PinKeyDerivation
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.inject.Inject

/** Wraps the vault key with the PIN or a fingerprint-authenticated cipher, and back. */
internal class VaultKeyWrapper @Inject constructor(private val keyDerivation: PinKeyDerivation) {

    suspend fun wrapWithPin(vaultKey: ByteArray, pin: CharArray): PinWrappedKey {
        val params = KdfParams.Default
        val salt = keyDerivation.newSalt()
        val pinKey = keyDerivation.derive(pin, salt, params)
        try {
            return PinWrappedKey(
                salt = salt.encode(),
                memoryKib = params.memoryKib,
                iterations = params.iterations,
                parallelism = params.parallelism,
                sealedKey = AesGcm.seal(pinKey, vaultKey, PIN_AAD).encode(),
            )
        } finally {
            pinKey.fill(0)
        }
    }

    /** The vault key, or null when [pin] is wrong (the GCM tag doesn't match). */
    suspend fun unwrapWithPin(wrapped: PinWrappedKey, pin: CharArray): ByteArray? {
        val params = KdfParams(wrapped.memoryKib, wrapped.iterations, wrapped.parallelism)
        val pinKey = keyDerivation.derive(pin, wrapped.salt.decode(), params)
        return try {
            AesGcm.open(pinKey, wrapped.sealedKey.decode(), PIN_AAD)
        } catch (_: AEADBadTagException) {
            null
        } finally {
            pinKey.fill(0)
        }
    }

    /** [cipher] is an encrypt cipher that BiometricPrompt authenticated. */
    fun wrapWithBiometric(vaultKey: ByteArray, cipher: Cipher): BiometricWrappedKey {
        cipher.updateAAD(BIOMETRIC_AAD)
        val sealed = cipher.doFinal(vaultKey)
        return BiometricWrappedKey(iv = cipher.iv.encode(), sealedKey = sealed.encode())
    }

    /** [cipher] is a decrypt cipher for [wrapped]'s IV that BiometricPrompt authenticated. */
    fun unwrapWithBiometric(wrapped: BiometricWrappedKey, cipher: Cipher): ByteArray {
        cipher.updateAAD(BIOMETRIC_AAD)
        return cipher.doFinal(wrapped.sealedKey.decode())
    }

    private companion object {
        // What each sealed copy of the vault key is, so one can't stand in for the other.
        val PIN_AAD = "vigil.vault-key.pin".encodeToByteArray()
        val BIOMETRIC_AAD = "vigil.vault-key.biometric".encodeToByteArray()
    }
}

internal fun ByteArray.encode(): String = Base64.getEncoder().encodeToString(this)

internal fun String.decode(): ByteArray = Base64.getDecoder().decode(this)
