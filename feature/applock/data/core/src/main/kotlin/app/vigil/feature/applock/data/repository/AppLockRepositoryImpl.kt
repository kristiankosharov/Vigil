package app.vigil.feature.applock.data.repository

import androidx.datastore.core.DataStore
import app.vigil.common.core.AppResult
import app.vigil.common.core.map
import app.vigil.common.datastore.asStoreResult
import app.vigil.common.datastore.safeStoreCall
import app.vigil.common.datastore.toStoreError
import app.vigil.common.security.AesGcm
import app.vigil.common.security.BiometricKeyInvalidatedException
import app.vigil.common.security.BiometricKeys
import app.vigil.common.security.VaultKeyControl
import app.vigil.feature.applock.data.AppLockRepository
import app.vigil.feature.applock.data.AppLockStateData
import app.vigil.feature.applock.data.BiometricCipherData
import app.vigil.feature.applock.data.PinCheckData
import app.vigil.feature.applock.data.local.AppLockSettings
import app.vigil.feature.applock.data.local.VaultKeyWrapper
import app.vigil.feature.applock.data.local.decode
import app.vigil.feature.applock.data.session.AppLockSession
import app.vigil.feature.applock.data.session.BiometricAvailability
import java.security.GeneralSecurityException
import java.security.ProviderException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

internal class AppLockRepositoryImpl @Inject constructor(
    private val store: DataStore<AppLockSettings>,
    private val wrapper: VaultKeyWrapper,
    private val biometricKeys: BiometricKeys,
    private val vault: VaultKeyControl,
    private val session: AppLockSession,
    private val biometric: BiometricAvailability,
    private val attempts: PinAttempts,
) : AppLockRepository {

    override fun observeLockState(): Flow<AppResult<AppLockStateData>> = combine(
        store.data.asStoreResult(),
        session.isLocked,
    ) { settings, locked ->
        settings.map {
            AppLockStateData(
                isLocked = locked && it.hasPin,
                isBiometricEnabled = it.biometricKey != null && biometric.isAvailable(),
                retryAfter = attempts.remainingLockout(it),
            )
        }
    }.distinctUntilChanged()

    override suspend fun createPin(pin: CharArray, biometric: BiometricCipherData?): AppResult<Unit> = safeStoreCall {
        val vaultKey = vault.copyKey() ?: AesGcm.newKey() // a new PIN while unlocked keeps the key
        try {
            val pinKey = wrapper.wrapWithPin(vaultKey, pin)
            val biometricKey = biometric?.let { wrapper.wrapWithBiometric(vaultKey, it.cipher) }
            if (biometricKey == null) biometricKeys.delete()
            store.updateData { AppLockSettings(pinKey = pinKey, biometricKey = biometricKey) }
            session.unlock(vaultKey)
        } finally {
            vaultKey.fill(0)
        }
    }

    override suspend fun unlockWithPin(pin: CharArray): AppResult<PinCheckData> = safeStoreCall { attempts.check(pin) }

    override fun prepareBiometricEnrollment(): AppResult<BiometricCipherData> = try {
        AppResult.Success(BiometricCipherData(biometricKeys.newEncryptCipher()))
    } catch (e: GeneralSecurityException) {
        AppResult.Failure(e.toStoreError()) // Keystore failures map and log like store failures
    } catch (e: ProviderException) {
        AppResult.Failure(e.toStoreError())
    }

    override suspend fun prepareBiometricUnlock(): AppResult<BiometricCipherData?> = safeStoreCall {
        store.data.first().biometricKey?.let { wrapped ->
            try {
                BiometricCipherData(biometricKeys.decryptCipher(wrapped.iv.decode()))
            } catch (_: BiometricKeyInvalidatedException) {
                // A fingerprint was added or removed: that key is gone for good, so is fingerprint unlock.
                biometricKeys.delete()
                store.updateData { it.copy(biometricKey = null) }
                null
            }
        }
    }

    override suspend fun unlockWithBiometric(biometric: BiometricCipherData): AppResult<Unit> = safeStoreCall {
        val wrapped = checkNotNull(store.data.first().biometricKey) { "Fingerprint unlock is off" }
        val vaultKey = wrapper.unwrapWithBiometric(wrapped, biometric.cipher)
        store.updateData { it.copy(failedAttempts = 0, lockout = null) }
        session.unlock(vaultKey)
        vaultKey.fill(0)
    }

    override fun lockNow() = session.lock()

    override fun onAppBackgrounded() = session.onBackgrounded()

    override fun onAppForegrounded() = session.onForegrounded()

    override fun isBiometricAvailable(): Boolean = biometric.isAvailable()
}
