package app.vigil.feature.settings.data.repository

import app.cash.turbine.test
import app.vigil.common.core.AppResult
import app.vigil.common.datastore.encryptedDataStore
import app.vigil.feature.settings.data.ThemeModeData
import app.vigil.feature.settings.data.local.SettingsPreferences
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin

class SettingsRepositoryImplTest : FunSpec({
    AeadConfig.register()
    val aead: Aead = newAead()

    test("the theme follows the system until the user picks one") {
        withRepository(File(tempdir(), "settings_preferences.json"), aead) { repository ->
            repository.observeThemeMode().test {
                awaitItem() shouldBe AppResult.Success(ThemeModeData.System)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("a chosen theme is emitted and survives a restart") {
        val file = File(tempdir(), "settings_preferences.json")
        withRepository(file, aead) { repository ->
            repository.observeThemeMode().test {
                awaitItem() shouldBe AppResult.Success(ThemeModeData.System)

                repository.setThemeMode(ThemeModeData.Dark) shouldBe AppResult.Success(Unit)
                awaitItem() shouldBe AppResult.Success(ThemeModeData.Dark)
                cancelAndIgnoreRemainingEvents()
            }
        }

        withRepository(file, aead) { repository ->
            repository.observeThemeMode().test {
                awaitItem() shouldBe AppResult.Success(ThemeModeData.Dark)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("every mode round-trips through the store") {
        withRepository(File(tempdir(), "settings_preferences.json"), aead) { repository ->
            ThemeModeData.entries.forEach { mode ->
                repository.setThemeMode(mode)
                repository.observeThemeMode().test {
                    awaitItem() shouldBe AppResult.Success(mode)
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    test("a file from an older version without the theme field gets the default") {
        val file = File(tempdir(), "settings_preferences.json")
        file.writeText("{}") // what a store without any fields wrote; plainAead leaves it readable
        withRepository(file, plainAead) { repository ->
            repository.observeThemeMode().test {
                awaitItem() shouldBe AppResult.Success(ThemeModeData.System)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("a file written under another file name resets to the default") {
        val dir = tempdir()
        val original = File(dir, "settings_preferences.json")
        withRepository(original, aead) { repository -> repository.setThemeMode(ThemeModeData.Light) }
        val renamed = File(dir, "other_preferences.json")
        original.copyTo(renamed)

        withRepository(renamed, aead) { repository ->
            repository.observeThemeMode().test {
                awaitItem() shouldBe AppResult.Success(ThemeModeData.System)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    test("a file written with another key resets to the default") {
        val file = File(tempdir(), "settings_preferences.json")
        withRepository(file, aead) { repository -> repository.setThemeMode(ThemeModeData.Light) }

        withRepository(file, newAead()) { repository ->
            repository.observeThemeMode().test {
                awaitItem() shouldBe AppResult.Success(ThemeModeData.System)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
})

private val plainAead = object : Aead { // no encryption, so a test can write the file by hand
    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray) = plaintext
    override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray) = ciphertext
}

private fun newAead(): Aead = KeysetHandle.newBuilder()
    .addEntry(
        KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM).withRandomId().makePrimary(),
    )
    .build()
    .getPrimitive(RegistryConfiguration.get(), Aead::class.java)

private suspend fun withRepository(file: File, aead: Aead, block: suspend (SettingsRepositoryImpl) -> Unit) {
    val job = Job()
    val store = encryptedDataStore(
        file,
        SettingsPreferences.serializer(),
        SettingsPreferences(),
        aead,
        CoroutineScope(Dispatchers.IO + job),
    )
    try {
        block(SettingsRepositoryImpl(store))
    } finally {
        job.cancelAndJoin()
    }
}
