package app.vigil.common.security

import app.vigil.common.core.IoDispatcher
import java.security.SecureRandom
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

/** Argon2id cost, stored next to each PIN-wrapped key so it can be raised later without breaking old ones. */
data class KdfParams(val memoryKib: Int, val iterations: Int, val parallelism: Int) {
    companion object {
        /** OWASP's Argon2id baseline (19 MiB, 2 passes, 1 lane): a few hundred ms on a phone. */
        val Default = KdfParams(memoryKib = 19 * 1024, iterations = 2, parallelism = 1)
    }
}

/**
 * Turns a PIN into a 256-bit key: `HMAC(device pepper, Argon2id(pin, salt))`. The pepper is a
 * Keystore key that can't leave the device, so a guess can only be checked on this phone; a copy of
 * the files can't be brute-forced elsewhere. The PIN itself is never stored.
 */
interface PinKeyDerivation {
    fun newSalt(): ByteArray

    suspend fun derive(pin: CharArray, salt: ByteArray, params: KdfParams): ByteArray
}

internal class PinKeyDerivationImpl @Inject constructor(
    private val pepper: DevicePepper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PinKeyDerivation {
    private val random = SecureRandom()

    override fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)

    override suspend fun derive(pin: CharArray, salt: ByteArray, params: KdfParams): ByteArray =
        withContext(dispatcher) {
            val stretched = ByteArray(KEY_BYTES)
            try {
                Argon2BytesGenerator().apply {
                    init(
                        Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                            .withSalt(salt)
                            .withMemoryAsKB(params.memoryKib)
                            .withIterations(params.iterations)
                            .withParallelism(params.parallelism)
                            .build(),
                    )
                }.generateBytes(pin, stretched)
                pepper.mac(stretched)
            } finally {
                stretched.fill(0)
            }
        }

    private companion object {
        const val SALT_BYTES = 16
        const val KEY_BYTES = 32
    }
}
