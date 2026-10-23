package app.vigil.feature.applock.data.repository

import app.cash.turbine.test
import app.vigil.common.core.AppResult
import app.vigil.common.datastore.encryptedDataStore
import app.vigil.feature.applock.data.AppLockStateData
import app.vigil.feature.applock.data.local.AppLockSettings
import app.vigil.feature.applock.data.local.PinHasher
import app.vigil.feature.applock.data.session.AppLockSession
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import java.io.File
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin

class AppLockRepositoryImplTest : FunSpec({
    AeadConfig.register()
    val aead: Aead = KeysetHandle.newBuilder()
        .addEntry(
            KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM).withRandomId().makePrimary(),
        )
        .build()
        .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

    test("without a PIN the app is never locked") {
        withRepository(File(tempdir(), "applock_settings.json"), aead) { repository, _ ->
            repository.observeLockState().test {
                awaitItem() shouldBe AppResult.Success(AppLockStateData(isLocked = false, isBiometricEnabled = false))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("creating a PIN unlocks the session; after a restart the app is locked") {
        val file = File(tempdir(), "applock_settings.json")
        withRepository(file, aead) { repository, session ->
            repository.createPin("1234", biometricEnabled = false) shouldBe AppResult.Success(Unit)
            session.isLocked.value shouldBe false
        }

        withRepository(file, aead) { repository, _ ->
            repository.observeLockState().test {
                awaitItem() shouldBe AppResult.Success(AppLockStateData(isLocked = true, isBiometricEnabled = false))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("a wrong PIN keeps it locked; the right one unlocks it") {
        val file = File(tempdir(), "applock_settings.json")
        withRepository(file, aead) { repository, _ -> repository.createPin("1234", biometricEnabled = false) }

        withRepository(file, aead) { repository, session ->
            repository.unlockWithPin("0000") shouldBe AppResult.Success(false)
            session.isLocked.value shouldBe true

            repository.unlockWithPin("1234") shouldBe AppResult.Success(true)
            session.isLocked.value shouldBe false
        }
    }

    test("biometric unlock is offered only when enabled and available") {
        withRepository(File(tempdir(), "applock_settings.json"), aead, biometricAvailable = false) { repository, _ ->
            repository.createPin("1234", biometricEnabled = true)
            repository.observeLockState().test {
                awaitItem() shouldBe AppResult.Success(AppLockStateData(isLocked = false, isBiometricEnabled = false))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("the PIN itself is never written to the file") {
        val file = File(tempdir(), "applock_settings.json")
        val plainAead = object : Aead { // no encryption, so the test can read what the store wrote
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) = plaintext
            override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray) = ciphertext
        }
        withRepository(file, plainAead) { repository, _ -> repository.createPin("987654", biometricEnabled = false) }

        file.readText() shouldNotContain "987654"
    }
})

private suspend fun withRepository(
    file: File,
    aead: Aead,
    biometricAvailable: Boolean = true,
    block: suspend (AppLockRepositoryImpl, AppLockSession) -> Unit,
) {
    val job = Job()
    val store =
        encryptedDataStore(
            file,
            AppLockSettings.serializer(),
            AppLockSettings(),
            aead,
            CoroutineScope(Dispatchers.IO + job),
        )
    val session = AppLockSession(TestTimeSource())
    try {
        block(AppLockRepositoryImpl(store, PinHasher(Dispatchers.Default), session) { biometricAvailable }, session)
    } finally {
        job.cancelAndJoin()
    }
}
