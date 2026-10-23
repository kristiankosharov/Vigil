package app.vigil.common.datastore.di

import android.content.Context
import app.vigil.common.datastore.DataStoreAead
import app.vigil.common.datastore.LazyAead
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Permanent names: renaming any of them makes every encrypted store unreadable.
private const val KEYSET_NAME = "datastore_keyset"
private const val KEYSET_PREFS_FILE = "datastore_keyset_prefs"
private const val MASTER_KEY_URI = "android-keystore://datastore_master_key"

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreCryptoModule {
    /**
     * One AES-256-GCM keyset for every store. The keyset lives in [KEYSET_PREFS_FILE], encrypted by a
     * master key in the Android Keystore (Tink's own storage; the only SharedPreferences allowed).
     */
    @Provides
    @Singleton
    @DataStoreAead
    fun provideAead(@ApplicationContext context: Context): Aead = LazyAead {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS_FILE)
            .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}
