package app.vigil.common.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.security.GeneralSecurityException

class AesGcmTest : FunSpec({
    val key = AesGcm.newKey()
    val aad = "tokens".encodeToByteArray()

    test("sealed data opens with the same key and associated data") {
        val sealed = AesGcm.seal(key, "secret".encodeToByteArray(), aad)

        AesGcm.open(key, sealed, aad).decodeToString() shouldBe "secret"
    }

    test("every seal uses a new nonce") {
        AesGcm.seal(key, byteArrayOf(1), aad).toList() shouldNotBe AesGcm.seal(key, byteArrayOf(1), aad).toList()
    }

    test("a wrong key, other associated data or a flipped bit fail") {
        val sealed = AesGcm.seal(key, "secret".encodeToByteArray(), aad)

        shouldThrow<GeneralSecurityException> { AesGcm.open(AesGcm.newKey(), sealed, aad) }
        shouldThrow<GeneralSecurityException> { AesGcm.open(key, sealed, "documents".encodeToByteArray()) }
        val flipped = sealed.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        shouldThrow<GeneralSecurityException> { AesGcm.open(key, flipped, aad) }
        shouldThrow<GeneralSecurityException> { AesGcm.open(key, byteArrayOf(1, 2, 3), aad) }
    }
})
