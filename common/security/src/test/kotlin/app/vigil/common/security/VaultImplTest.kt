package app.vigil.common.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class VaultImplTest : FunSpec({
    val aad = "tokens".encodeToByteArray()

    test("a new vault is closed and refuses to seal") {
        val vault = VaultImpl()

        vault.isOpen.value shouldBe false
        shouldThrow<VaultLockedException> { vault.seal(byteArrayOf(1), aad) }
    }

    test("an open vault seals and unseals; closing wipes the key") {
        val vault = VaultImpl()
        val key = AesGcm.newKey()
        vault.open(key)

        val sealed = vault.seal("secret".encodeToByteArray(), aad)
        vault.unseal(sealed, aad).decodeToString() shouldBe "secret"
        vault.copyKey()?.toList() shouldBe key.toList()

        vault.close()
        vault.isOpen.value shouldBe false
        vault.copyKey().shouldBeNull()
        shouldThrow<VaultLockedException> { vault.unseal(sealed, aad) }
    }

    test("the vault keeps its own copy of the key") {
        val vault = VaultImpl()
        val key = AesGcm.newKey()
        val expected = key.copyOf()
        vault.open(key)
        key.fill(0)

        vault.copyKey()?.toList() shouldBe expected.toList()
    }
})
