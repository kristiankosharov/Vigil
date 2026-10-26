package app.vigil.feature.documents.data.di

import app.vigil.feature.documents.data.DocumentsRepository
import app.vigil.feature.documents.data.repository.DocumentsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DocumentsRepositoryModule {
    @Binds
    fun bindDocumentsRepository(impl: DocumentsRepositoryImpl): DocumentsRepository
}
