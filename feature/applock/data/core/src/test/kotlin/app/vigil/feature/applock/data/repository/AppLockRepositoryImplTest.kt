package app.vigil.feature.applock.data.repository

import app.cash.turbine.test
import app.vigil.common.core.AppResult
import app.vigil.common.core.UserDataCleaner
import app.vigil.common.datastore.encryptedDataStore
import app.vigil.common.network.SessionEvents
import app.vigil.common.security.BiometricKeyInvalidatedException
import app.vigil.common.security.BiometricKeys
import app.vigil.common.security.KdfParams
import app.vigil.common.security.PinKeyDerivation
import app.vigil.feature.applock.data.AppLockStateData
import app.vigil.feature.applock.data.BiometricCipherData
import app.vigil.feature.applock.data.FakeVault
import app.vigil.feature.applock.data.PinCheckData
import app.vigil.feature.applock.data.local.AppLockSettings
import app.vigil.feature.applock.data.local.VaultKeyWrapper
import app.vigil.feature.applock.data.session.AppLockSession
import app.vigil.feature.applock.data.session.DeviceClock
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class AppLockRepositoryImplTest : FunSpec({
    AeadConfig.register()
    val aead: Aead = KeysetHandle.newBuilder()
        .addEntry(
            KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM).withRandomId().makePrimary(),
        )
        .build()
        .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    val pin = "482916"
    val secret = "token".encodeToByteArray()
    val aad = "test".encodeToByteArray()

    fun file() = File(tempdir(), "applock_settings.json")

    test("without a PIN the app is never locked") {
        Device(file(), aead).run {
            repository.observeLockState().test {
                awaitItem() shouldBe AppResult.Success(AppLockStateData(isLocked = false, isBiometricEnabled = false))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("creating a PIN opens the vault; after a restart the app is locked and the vault closed") {
        val file = file()
        Device(file, aead).run {
            repository.createPin(pin.toCharArray(), biometric = null) shouldBe AppResult.Success(Unit)
            session.isLocked.value shouldBe false
            vault.isOpen.value shouldBe true
        }

        Device(file, aead).run {
            vault.isOpen.value shouldBe false
            repository.observeLockState().test {
                awaitItem() shouldBe AppResult.Success(AppLockStateData(isLocked = true, isBiometricEnabled = false))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("a wrong PIN can't open the vault; the right one opens it with the same key") {
        val file = file()
        val sealed = Device(file, aead).run {
            repository.createPin(pin.toCharArray(), biometric = null)
            vault.seal(secret, aad)
        }

        Device(file, aead).run {
            repository.unlockWithPin("000000".toCharArray()) shouldBe
                AppResult.Success(PinCheckData.Wrong(attemptsLeft = 9, retryAfter = Duration.ZERO))
            session.isLocked.value shouldBe true
            vault.isOpen.value shouldBe false

            repository.unlockWithPin(pin.toCharArray()) shouldBe AppResult.Success(PinCheckData.Correct)
            session.isLocked.value shouldBe false
            vault.unseal(sealed, aad).toList() shouldBe secret.toList()
        }
    }

    test("the fifth wrong PIN starts a wait; during it even the right PIN isn't tried; afterwards it works") {
        val file = file()
        Device(file, aead).run { repository.createPin(pin.toCharArray(), biometric = null) }

        Device(file, aead).run {
            repeat(4) { repository.unlockWithPin("000000".toCharArray()) }
            repository.unlockWithPin("000000".toCharArray()) shouldBe
                AppResult.Success(PinCheckData.Wrong(attemptsLeft = 5, retryAfter = 30.seconds))

            clock.now += 10.seconds
            repository.unlockWithPin(pin.toCharArray()) shouldBe
                AppResult.Success(PinCheckData.Wrong(attemptsLeft = 5, retryAfter = 20.seconds))
            session.isLocked.value shouldBe true

            clock.now += 20.seconds
            repository.unlockWithPin(pin.toCharArray()) shouldBe AppResult.Success(PinCheckData.Correct)
            store.data.first().failedAttempts shouldBe 0
        }
    }

    test("a wait that was running when the phone restarted starts again in full") {
        val file = file()
        Device(file, aead).run {
            repository.createPin(pin.toCharArray(), biometric = null)
            repeat(5) { repository.unlockWithPin("000000".toCharArray()) }
            clock.now += 30.seconds
            repository.unlockWithPin("000000".toCharArray()) // the sixth: 1 minute
        }

        Device(file, aead, clock = FakeClock(bootCount = 2)).run {
            repository.observeLockState().test {
                awaitItem().shouldBeInstanceOf<AppResult.Success<AppLockStateData>>().data.retryAfter shouldBe 1.minutes
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("the tenth wrong PIN erases everything and signs out") {
        val file = file()
        Device(file, aead).run {
            repository.createPin(pin.toCharArray(), biometric = null)
            repository.lockNow()
            repeat(9) {
                clock.now += 20.minutes // past every wait
                repository.unlockWithPin("000000".toCharArray())
            }
            clock.now += 20.minutes

            repository.unlockWithPin("000000".toCharArray()) shouldBe AppResult.Success(PinCheckData.Erased)
            cleaned shouldBe 1
            sessionEvents.notified shouldBe 1
            store.data.first().hasPin shouldBe false
            vault.isOpen.value shouldBe false
            biometricKeys.deleted shouldBe true
        }
    }

    test("fingerprint unlock opens the vault with the same key; a changed fingerprint set turns it off") {
        val file = file()
        val biometricKeys = FakeBiometricKeys()
        val sealed = Device(file, aead, biometricKeys = biometricKeys).run {
            val enrollment = repository.prepareBiometricEnrollment()
                .shouldBeInstanceOf<AppResult.Success<BiometricCipherData>>()
            repository.createPin(pin.toCharArray(), enrollment.data)
            vault.seal(secret, aad)
        }

        Device(file, aead, biometricKeys = biometricKeys).run {
            repository.observeLockState().test {
                awaitItem() shouldBe AppResult.Success(AppLockStateData(isLocked = true, isBiometricEnabled = true))
                cancelAndIgnoreRemainingEvents()
            }
            val cipher = repository.prepareBiometricUnlock()
                .shouldBeInstanceOf<AppResult.Success<BiometricCipherData?>>().data.shouldNotBeNull()
            repository.unlockWithBiometric(cipher) shouldBe AppResult.Success(Unit)
            vault.unseal(sealed, aad).toList() shouldBe secret.toList()

            biometricKeys.invalidated = true
            repository.prepareBiometricUnlock() shouldBe AppResult.Success(null)
            store.data.first().biometricKey.shouldBeNull()
        }
    }

    test("a new PIN while unlocked keeps the vault key") {
        val file = file()
        Device(file, aead).run {
            repository.createPin(pin.toCharArray(), biometric = null)
            val sealed = vault.seal(secret, aad)

            repository.createPin("739058".toCharArray(), biometric = null)
            repository.lockNow()
            repository.unlockWithPin("739058".toCharArray()) shouldBe AppResult.Success(PinCheckData.Correct)
            vault.unseal(sealed, aad).toList() shouldBe secret.toList()
        }
    }

    test("neither the PIN nor the vault key is written to the file") {
        val file = file()
        val plainAead = object : Aead { // no encryption, so the test can read what the store wrote
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) = plaintext
            override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray) = ciphertext
        }
        val key = Device(file, plainAead).run {
            repository.createPin(pin.toCharArray(), biometric = null)
            vault.copyKey().shouldNotBeNull()
        }

        file.readText().let {
            it shouldNotContain pin
            it shouldNotContain java.util.Base64.getEncoder().encodeToString(key)
        }
    }
})

private class FakeClock(var now: Duration = 1.minutes, private val bootCount: Int = 1) : DeviceClock {
    override fun sinceBoot() = now

    override fun bootCount() = bootCount
}

/** Fast and deterministic in place of Argon2id + the Keystore pepper. */
private object FakeKeyDerivation : PinKeyDerivation {
    override fun newSalt() = ByteArray(16).also(SecureRandom()::nextBytes)

    override suspend fun derive(pin: CharArray, salt: ByteArray, params: KdfParams): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(salt, "HmacSHA256"))
            doFinal(String(pin).encodeToByteArray())
        }
}

/** A software AES key in place of the biometric-bound Keystore key (no prompt on the JVM). */
private class FakeBiometricKeys : BiometricKeys {
    private var key: SecretKeySpec? = null
    var invalidated = false
    var deleted = false

    override fun newEncryptCipher(): Cipher {
        key = SecretKeySpec(ByteArray(32).also(SecureRandom()::nextBytes), "AES")
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
    }

    override fun decryptCipher(iv: ByteArray): Cipher {
        if (invalidated) throw BiometricKeyInvalidatedException()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
    }

    override fun delete() {
        deleted = true
    }
}

private class FakeSessionEvents : SessionEvents {
    var notified = 0
    override val expired = MutableSharedFlow<Unit>()

    override fun notifyExpired() {
        notified++
    }
}

/** One app process: a new Device on the same file is a restart (new session, closed vault). */
private class Device(
    file: File,
    aead: Aead,
    val clock: FakeClock = FakeClock(),
    val biometricKeys: FakeBiometricKeys = FakeBiometricKeys(),
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
    val vault = FakeVault()
    val session = AppLockSession(TestTimeSource(), vault, CoroutineScope(Dispatchers.IO + job))
    val sessionEvents = FakeSessionEvents()
    var cleaned = 0
    val wrapper = VaultKeyWrapper(FakeKeyDerivation)
    val attempts = PinAttempts(
        store = store,
        wrapper = wrapper,
        session = session,
        biometricKeys = biometricKeys,
        clock = clock,
        cleaners = {
            setOf(
                UserDataCleaner {
                    cleaned++
                    store.updateData { AppLockSettings() }
                },
            )
        },
        sessionEvents = sessionEvents,
    )
    val repository = AppLockRepositoryImpl(
        store = store,
        wrapper = wrapper,
        biometricKeys = biometricKeys,
        vault = vault,
        session = session,
        biometric = { true },
        attempts = attempts,
    )

    suspend fun <T> run(block: suspend Device.() -> T): T = try {
        block()
    } finally {
        job.cancelAndJoin()
    }
}
