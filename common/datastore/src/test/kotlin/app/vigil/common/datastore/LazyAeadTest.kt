package app.vigil.common.datastore

import app.vigil.common.core.AppError
import com.google.crypto.tink.Aead
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.ProviderException

class LazyAeadTest : FunSpec({

    test("a GeneralSecurityException during key setup surfaces as IOException") {
        val aead = LazyAead { throw GeneralSecurityException("keystore busy") }

        shouldThrow<IOException> { aead.encrypt(byteArrayOf(1), byteArrayOf()) }
            .cause.shouldBeInstanceOf<GeneralSecurityException>()
    }

    test("a ProviderException during key setup surfaces as IOException") {
        val aead = LazyAead { throw ProviderException("keystore unavailable") }

        shouldThrow<IOException> { aead.decrypt(byteArrayOf(1), byteArrayOf()) }
            .cause.shouldBeInstanceOf<ProviderException>()
    }

    test("a failed setup is retried on the next access, then the key is created only once") {
        var attempts = 0
        val aead = LazyAead {
            attempts++
            if (attempts == 1) throw GeneralSecurityException("first try fails")
            ReversingAead
        }

        shouldThrow<IOException> { aead.encrypt(byteArrayOf(1, 2), byteArrayOf()) }
        aead.encrypt(byteArrayOf(1, 2), byteArrayOf()).toList() shouldBe listOf<Byte>(2, 1)
        aead.decrypt(byteArrayOf(2, 1), byteArrayOf()).toList() shouldBe listOf<Byte>(1, 2)
        attempts shouldBe 2
    }

    test("an IOException maps to AppError.Local.Storage") {
        IOException("key unavailable").toStoreError()
            .shouldBeInstanceOf<AppError.Local.Storage>()
    }
})

private object ReversingAead : Aead {
    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) = plaintext.reversedArray()

    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray) = ciphertext.reversedArray()
}
