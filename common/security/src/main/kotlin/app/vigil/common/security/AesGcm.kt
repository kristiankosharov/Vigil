package app.vigil.common.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM in software for keys the app holds itself (the vault key, document keys). Output is
 * `nonce (12 bytes) ‖ ciphertext ‖ tag (16 bytes)`, with a new random nonce every time.
 */
object AesGcm {
    const val KEY_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val random = SecureRandom()

    fun newKey(): ByteArray = ByteArray(KEY_BYTES).also(random::nextBytes)

    fun seal(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray): ByteArray {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(associatedData)
        }
        return nonce + cipher.doFinal(plaintext)
    }

    /** Throws `AEADBadTagException` for a wrong key, or tampered data or [associatedData]. */
    fun open(key: ByteArray, sealed: ByteArray, associatedData: ByteArray): ByteArray {
        if (sealed.size < NONCE_BYTES + TAG_BITS / Byte.SIZE_BITS) throw GeneralSecurityException("Too short")
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, sealed, 0, NONCE_BYTES))
            updateAAD(associatedData)
        }
        return cipher.doFinal(sealed, NONCE_BYTES, sealed.size - NONCE_BYTES)
    }
}
