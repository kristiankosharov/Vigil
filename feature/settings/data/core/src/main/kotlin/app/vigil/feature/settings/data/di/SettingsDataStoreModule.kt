package app.vigil.feature.settings.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import app.vigil.common.core.ApplicationScope
import app.vigil.common.datastore.DataStoreAead
import app.vigil.common.datastore.encryptedDataStore
import app.vigil.feature.settings.data.local.SettingsPreferences
import com.google.crypto.tink.Aead
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

// No UserDataCleaner: these are device settings (like the theme) and stay after logout.
@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDataStoreModule {
    @Provides
    @Singleton
    fun provideSettingsPreferencesStore(
        @ApplicationContext context: Context,
        @DataStoreAead aead: Aead,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<SettingsPreferences> = context.encryptedDataStore(
        fileName = SETTINGS_PREFERENCES_FILE,
        serializer = SettingsPreferences.serializer(),
        defaultValue = SettingsPreferences(),
        aead = aead,
        scope = scope,
    )
}

/** Permanent: also the AEAD associated data. */
internal const val SETTINGS_PREFERENCES_FILE = "settings_preferences.json"
