package app.vigil.feature.applock.data

import app.vigil.common.security.AesGcm
import app.vigil.common.security.Vault
import app.vigil.common.security.VaultKeyControl
import app.vigil.common.security.VaultLockedException
import kotlinx.coroutines.flow.MutableStateFlow

/** An in-memory vault like the real one (VaultImpl is internal to :common:security). */
internal class FakeVault :
    Vault,
    VaultKeyControl {
    private var key: ByteArray? = null
    override val isOpen = MutableStateFlow(false)

    override fun open(key: ByteArray) {
        this.key = key.copyOf()
        isOpen.value = true
    }

    override fun close() {
        key = null
        isOpen.value = false
    }

    override fun copyKey(): ByteArray? = key?.copyOf()

    override fun seal(plaintext: ByteArray, associatedData: ByteArray) =
        AesGcm.seal(key ?: throw VaultLockedException(), plaintext, associatedData)

    override fun unseal(sealed: ByteArray, associatedData: ByteArray) =
        AesGcm.open(key ?: throw VaultLockedException(), sealed, associatedData)
}
