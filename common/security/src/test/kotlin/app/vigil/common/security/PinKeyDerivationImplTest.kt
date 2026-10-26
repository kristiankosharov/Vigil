package app.vigil.common.security

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers

class PinKeyDerivationImplTest : FunSpec({
    // A software stand-in for the Keystore HMAC key.
    fun pepper(secret: Byte) = DevicePepper { input ->
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(ByteArray(32) { secret }, "HmacSHA256"))
            doFinal(input)
        }
    }

    val params = KdfParams(memoryKib = 1024, iterations = 1, parallelism = 1) // fast for tests
    val derivation = PinKeyDerivationImpl(pepper(1), Dispatchers.Unconfined)
    val salt = derivation.newSalt()

    test("the same PIN, salt and pepper give the same 256-bit key") {
        val first = derivation.derive("123456".toCharArray(), salt, params)

        first.size shouldBe 32
        derivation.derive("123456".toCharArray(), salt, params).toList() shouldBe first.toList()
    }

    test("another PIN, salt or device gives another key") {
        val key = derivation.derive("123456".toCharArray(), salt, params).toList()

        derivation.derive("123457".toCharArray(), salt, params).toList() shouldNotBe key
        derivation.derive("123456".toCharArray(), derivation.newSalt(), params).toList() shouldNotBe key
        PinKeyDerivationImpl(pepper(2), Dispatchers.Unconfined)
            .derive("123456".toCharArray(), salt, params).toList() shouldNotBe key
    }
})
