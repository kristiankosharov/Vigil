package app.vigil.feature.documents.data.di

import androidx.datastore.core.DataStore
import app.vigil.common.core.UserDataCleaner
import app.vigil.feature.documents.data.local.DocumentCrypto
import app.vigil.feature.documents.data.local.DocumentFiles
import app.vigil.feature.documents.data.local.DocumentsIndex
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** On logout (or the last wrong PIN): the index, the pages and the RSA key pair all go. */
@Module
@InstallIn(SingletonComponent::class)
internal object DocumentsDataCleanerModule {
    @Provides
    @IntoSet
    fun provideDocumentsDataCleaner(
        store: DataStore<DocumentsIndex>,
        files: DocumentFiles,
        crypto: DocumentCrypto,
    ): UserDataCleaner = UserDataCleaner {
        store.updateData { DocumentsIndex() }
        files.deleteAll()
        crypto.deleteKeys()
    }
}
