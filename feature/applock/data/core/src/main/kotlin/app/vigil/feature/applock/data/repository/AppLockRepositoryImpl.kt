package app.vigil.feature.applock.data.repository

import androidx.datastore.core.DataStore
import app.vigil.common.core.AppResult
import app.vigil.common.core.map
import app.vigil.common.core.onSuccess
import app.vigil.common.datastore.asStoreResult
import app.vigil.common.datastore.safeStoreCall
import app.vigil.feature.applock.data.AppLockRepository
import app.vigil.feature.applock.data.AppLockStateData
import app.vigil.feature.applock.data.local.AppLockSettings
import app.vigil.feature.applock.data.local.PinHasher
import app.vigil.feature.applock.data.session.AppLockSession
import app.vigil.feature.applock.data.session.BiometricAvailability
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

internal class AppLockRepositoryImpl @Inject constructor(
    private val store: DataStore<AppLockSettings>,
    private val hasher: PinHasher,
    private val session: AppLockSession,
    private val biometric: BiometricAvailability,
) : AppLockRepository {

    override fun observeLockState(): Flow<AppResult<AppLockStateData>> = combine(
        store.data.asStoreResult(),
        session.isLocked,
    ) { settings, locked ->
        settings.map {
            AppLockStateData(
                isLocked = locked && it.hasPin,
                isBiometricEnabled = it.biometricEnabled && biometric.isAvailable(),
            )
        }
    }.distinctUntilChanged()

    override suspend fun createPin(pin: String, biometricEnabled: Boolean): AppResult<Unit> = safeStoreCall {
        val salt = hasher.newSalt()
        val hash = hasher.hash(pin, salt)
        store.updateData {
            AppLockSettings(
                pinHash = hash.encode(),
                pinSalt = salt.encode(),
                pinIterations = PinHasher.ITERATIONS,
                biometricEnabled = biometricEnabled,
            )
        }
    }.onSuccess { session.unlock() }.map { }

    override suspend fun unlockWithPin(pin: String): AppResult<Boolean> = safeStoreCall {
        val settings = store.data.first()
        val expected = settings.pinHash?.decode()
        val salt = settings.pinSalt?.decode()
        expected != null && salt != null && hasher.matches(expected, hasher.hash(pin, salt, settings.pinIterations))
    }.onSuccess { matches -> if (matches) session.unlock() }

    override fun unlockWithBiometric() = session.unlock()

    override fun lockNow() = session.lock()

    override fun onAppBackgrounded() = session.onBackgrounded()

    override fun onAppForegrounded() = session.onForegrounded()

    override fun isBiometricAvailable(): Boolean = biometric.isAvailable()
}

private fun ByteArray.encode(): String = Base64.getEncoder().encodeToString(this)

private fun String.decode(): ByteArray = Base64.getDecoder().decode(this)
