package app.vigil.common.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.inject.Inject

/**
 * Envelope encryption's outer layer: wraps small data keys with an RSA-3072 key pair in the Android
 * Keystore (RSA-OAEP, SHA-256). Wrapping needs only the public key; unwrapping runs inside the
 * Keystore, whose private key never leaves it. Each [alias] is its own key pair, created on first use.
 */
interface RsaKeyWrapper {
    fun wrap(alias: String, key: ByteArray): ByteArray

    /** Throws a [GeneralSecurityException] when the key pair is gone or [wrapped] isn't from it. */
    fun unwrap(alias: String, wrapped: ByteArray): ByteArray

    fun delete(alias: String)
}

internal class RsaKeyWrapperImpl @Inject constructor() : RsaKeyWrapper {

    override fun wrap(alias: String, key: ByteArray): ByteArray = Cipher.getInstance(TRANSFORMATION).run {
        init(Cipher.ENCRYPT_MODE, publicKey(alias), Oaep)
        doFinal(key)
    }

    override fun unwrap(alias: String, wrapped: ByteArray): ByteArray {
        val privateKey = keyStore().getKey(alias, null) as? PrivateKey
            ?: throw GeneralSecurityException("No key pair $alias")
        return Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, privateKey, Oaep)
            doFinal(wrapped)
        }
    }

    override fun delete(alias: String) {
        keyStore().deleteEntry(alias)
    }

    private fun keyStore() = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** A software copy of the public key: encrypting needs no Keystore call. */
    private fun publicKey(alias: String): PublicKey {
        val certificate = keyStore().getCertificate(alias) ?: generate(alias).let { keyStore().getCertificate(alias) }
        return KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
            .generatePublic(X509EncodedKeySpec(certificate.publicKey.encoded))
    }

    private fun generate(alias: String) =
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE).run {
            initialize(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(KEY_BITS)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .build(),
            )
            generateKeyPair()
        }

    private companion object {
        const val KEY_BITS = 3072 // StrongBox only guarantees RSA-2048, so this key lives in the TEE
        const val TRANSFORMATION = "RSA/ECB/OAEPPadding"

        // MGF1 stays SHA-1: the Keystore supports only that before Android 15 (still sound for OAEP).
        val Oaep = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
    }
}
