package app.vigil.feature.applock.data.local

import app.vigil.common.core.IoDispatcher
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * PBKDF2-HMAC-SHA256 with a random salt per PIN. A short PIN can't resist offline brute force on
 * its own; the real protection is the encrypted store (Keystore key). The hash only keeps the PIN
 * itself out of the file.
 */
internal class PinHasher @Inject constructor(@param:IoDispatcher private val dispatcher: CoroutineDispatcher) {

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)

    suspend fun hash(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray = withContext(dispatcher) {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison, so timing doesn't reveal how much of the hash matched. */
    fun matches(expected: ByteArray, actual: ByteArray): Boolean = MessageDigest.isEqual(expected, actual)

    companion object {
        const val ITERATIONS = 120_000
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
    }
}
