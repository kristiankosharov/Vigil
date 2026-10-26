package app.vigil.feature.documents.data.di

import android.content.Context
import app.vigil.feature.documents.data.local.DocumentFiles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
internal object DocumentsFilesModule {
    /** Never backed up: without this device's Keystore the pages can't be decrypted anyway. */
    @Provides
    fun provideDocumentFiles(@ApplicationContext context: Context) =
        DocumentFiles(File(context.noBackupFilesDir, DOCUMENTS_DIR))
}

private const val DOCUMENTS_DIR = "documents"
