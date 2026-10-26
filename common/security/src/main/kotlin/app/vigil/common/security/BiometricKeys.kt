package app.vigil.common.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/**
 * A Keystore AES-GCM key that works only right after a strong-biometric check (each use goes through
 * BiometricPrompt with a CryptoObject). Adding or removing a fingerprint destroys it.
 */
interface BiometricKeys {
    /** Replaces any old key and returns a cipher that encrypts once BiometricPrompt has authenticated it. */
    fun newEncryptCipher(): Cipher

    /** A cipher that decrypts what [newEncryptCipher] produced (with its [iv]) once authenticated. */
    fun decryptCipher(iv: ByteArray): Cipher

    fun delete()
}

/** The fingerprint key is gone (a fingerprint was added or removed): unlock with the PIN instead. */
class BiometricKeyInvalidatedException(cause: Throwable? = null) :
    GeneralSecurityException("The biometric key is no longer valid", cause)

internal class BiometricKeysImpl @Inject constructor() : BiometricKeys {

    override fun newEncryptCipher(): Cipher {
        delete()
        return Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, generate()) }
    }

    override fun decryptCipher(iv: ByteArray): Cipher {
        val key = keyStore().getKey(ALIAS, null) as? SecretKey ?: throw BiometricKeyInvalidatedException()
        return try {
            Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv)) }
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw BiometricKeyInvalidatedException(e)
        }
    }

    override fun delete() {
        keyStore().deleteEntry(ALIAS)
    }

    private fun keyStore() = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun generate(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG) // every use
                        } else {
                            @Suppress("DEPRECATION")
                            setUserAuthenticationValidityDurationSeconds(-1) // every use, biometrics only
                        }
                    }
                    .build(),
            )
            generateKey()
        }

    private companion object {
        const val ALIAS = "vault_biometric_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}
