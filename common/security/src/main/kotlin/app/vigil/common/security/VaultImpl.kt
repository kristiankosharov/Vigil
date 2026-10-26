package app.vigil.common.security

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
internal class VaultImpl @Inject constructor() :
    Vault,
    VaultKeyControl {
    private val lock = Any()
    private var key: ByteArray? = null
    private val open = MutableStateFlow(false)

    override val isOpen: StateFlow<Boolean> = open.asStateFlow()

    override fun open(key: ByteArray) {
        synchronized(lock) {
            this.key?.fill(0)
            this.key = key.copyOf()
        }
        open.value = true
    }

    override fun close() {
        synchronized(lock) {
            key?.fill(0)
            key = null
        }
        open.value = false
    }

    override fun copyKey(): ByteArray? = synchronized(lock) { key?.copyOf() }

    override fun seal(plaintext: ByteArray, associatedData: ByteArray): ByteArray =
        withKey { AesGcm.seal(it, plaintext, associatedData) }

    override fun unseal(sealed: ByteArray, associatedData: ByteArray): ByteArray =
        withKey { AesGcm.open(it, sealed, associatedData) }

    // A copy, so close() can wipe the key while a seal is still running.
    private inline fun <T> withKey(block: (ByteArray) -> T): T {
        val copy = copyKey() ?: throw VaultLockedException()
        return try {
            block(copy)
        } finally {
            copy.fill(0)
        }
    }
}
