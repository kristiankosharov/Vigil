package app.vigil.common.security

import java.security.GeneralSecurityException
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's data key while the app is unlocked: a random AES-256-GCM key that exists only in memory.
 * :feature:applock opens it with the PIN or fingerprint and closes it when the app locks; features
 * seal their secrets with it (the backend tokens, document keys). It is never stored unwrapped.
 */
interface Vault {
    val isOpen: StateFlow<Boolean>

    /** Encrypts [plaintext]; [associatedData] names what it is, so sealed values can't be swapped. */
    fun seal(plaintext: ByteArray, associatedData: ByteArray): ByteArray

    /** Throws [VaultLockedException] while closed, or a [GeneralSecurityException] for tampered data. */
    fun unseal(sealed: ByteArray, associatedData: ByteArray): ByteArray
}

/** Opens and closes the [Vault]. Only :feature:applock uses it. */
interface VaultKeyControl {
    /** Keeps a copy of [key]; the caller wipes its own. */
    fun open(key: ByteArray)

    /** Wipes the key from memory. */
    fun close()

    /** A copy of the open key, to wrap it again (a new PIN, fingerprint); null while closed. */
    fun copyKey(): ByteArray?
}

class VaultLockedException : GeneralSecurityException("The vault is locked")
