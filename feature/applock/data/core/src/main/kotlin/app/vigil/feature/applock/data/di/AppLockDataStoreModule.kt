package app.vigil.feature.applock.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import app.vigil.common.core.ApplicationScope
import app.vigil.common.datastore.DataStoreAead
import app.vigil.common.datastore.encryptedDataStore
import app.vigil.feature.applock.data.local.AppLockSettings
import com.google.crypto.tink.Aead
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
internal object AppLockDataStoreModule {
    @Provides
    @Singleton
    fun provideAppLockSettingsStore(
        @ApplicationContext context: Context,
        @DataStoreAead aead: Aead,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<AppLockSettings> = context.encryptedDataStore(
        fileName = APPLOCK_SETTINGS_FILE,
        serializer = AppLockSettings.serializer(),
        defaultValue = AppLockSettings(),
        aead = aead,
        scope = scope,
    )
}

/** Permanent: also the AEAD associated data. */
internal const val APPLOCK_SETTINGS_FILE = "applock_settings.json"
