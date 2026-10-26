package app.vigil.common.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.inject.Inject

/** An HMAC with a key that never leaves this device's secure hardware. */
internal fun interface DevicePepper {
    fun mac(input: ByteArray): ByteArray
}

/** HMAC-SHA256 with a non-exportable Keystore key, in StrongBox when the phone has one, else the TEE. */
internal class DevicePepperImpl @Inject constructor() : DevicePepper {

    override fun mac(input: ByteArray): ByteArray = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256).run {
        init(key())
        doFinal(input)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(ALIAS, null) as? SecretKey ?: generate()
    }

    private fun generate(): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generate(strongBox = true)
            } catch (_: StrongBoxUnavailableException) {
                // no StrongBox on this phone: the TEE
            }
        }
        return generate(strongBox = false)
    }

    private fun generate(strongBox: Boolean): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                    .apply {
                        if (strongBox &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        ) {
                            setIsStrongBoxBacked(true)
                        }
                    }
                    .build(),
            )
            generateKey()
        }

    private companion object {
        const val ALIAS = "vault_pin_pepper_v1" // permanent: a new alias makes every PIN-wrapped key unreadable
    }
}

internal const val ANDROID_KEYSTORE = "AndroidKeyStore"
