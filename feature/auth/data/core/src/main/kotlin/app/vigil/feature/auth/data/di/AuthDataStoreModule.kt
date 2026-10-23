package app.vigil.feature.auth.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import app.vigil.common.core.ApplicationScope
import app.vigil.common.datastore.DataStoreAead
import app.vigil.common.datastore.encryptedDataStore
import app.vigil.feature.auth.data.local.AuthSession
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
internal object AuthDataStoreModule {
    @Provides
    @Singleton
    fun provideAuthSessionStore(
        @ApplicationContext context: Context,
        @DataStoreAead aead: Aead,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<AuthSession> = context.encryptedDataStore(
        fileName = AUTH_SESSION_FILE,
        serializer = AuthSession.serializer(),
        defaultValue = AuthSession(),
        aead = aead,
        scope = scope,
    )
}

/** Permanent: also the AEAD associated data. */
internal const val AUTH_SESSION_FILE = "auth_session.json"
