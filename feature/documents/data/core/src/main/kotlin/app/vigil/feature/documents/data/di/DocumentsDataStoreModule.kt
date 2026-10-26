package app.vigil.feature.documents.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import app.vigil.common.core.ApplicationScope
import app.vigil.common.datastore.DataStoreAead
import app.vigil.common.datastore.encryptedDataStore
import app.vigil.feature.documents.data.local.DocumentsIndex
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
internal object DocumentsDataStoreModule {
    @Provides
    @Singleton
    fun provideDocumentsIndexStore(
        @ApplicationContext context: Context,
        @DataStoreAead aead: Aead,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<DocumentsIndex> = context.encryptedDataStore(
        fileName = DOCUMENTS_INDEX_FILE,
        serializer = DocumentsIndex.serializer(),
        defaultValue = DocumentsIndex(),
        aead = aead,
        scope = scope,
    )
}

/** Permanent: also the AEAD associated data. */
internal const val DOCUMENTS_INDEX_FILE = "documents_index.json"
