package app.vigil.common.datastore

import com.google.crypto.tink.Aead
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException

/**
 * Creating the Keystore-backed keyset is slow, so it is deferred until the first encrypt/decrypt,
 * which DataStore always runs on its own IO scope (never on the main thread).
 *
 * Key setup failures are rethrown as IOException on purpose. AeadSerializer turns
 * GeneralSecurityException into CorruptionException, and the corruption handler would then wipe
 * the store. Android Keystore can fail temporarily, so a failed setup must be a retryable I/O
 * error (AppError.Local.Storage), never "corrupt data". Only a real decryption failure of the
 * file itself is corruption. `lazy` does not cache a thrown exception, so the next access retries.
 */
internal class LazyAead(create: () -> Aead) : Aead {
    private val delegate by lazy {
        try {
            create()
        } catch (e: GeneralSecurityException) {
            throw IOException("DataStore key unavailable", e)
        } catch (e: ProviderException) { // java.security.ProviderException (Keystore, unchecked)
            throw IOException("DataStore key unavailable", e)
        }
    }

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        delegate.encrypt(plaintext, associatedData)

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
        delegate.decrypt(ciphertext, associatedData)
}
