package app.vigil.feature.documents.domain.di

import app.vigil.common.core.mapResult
import app.vigil.feature.applock.data.AppLockRepository
import app.vigil.feature.documents.data.DocumentData
import app.vigil.feature.documents.data.DocumentsRepository
import app.vigil.feature.documents.domain.mapper.toDomain
import app.vigil.feature.documents.domain.usecase.LockWalletUseCase
import app.vigil.feature.documents.domain.usecase.ObserveDocumentsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Pure Kotlin module (hilt-core only): SingletonComponent, unscoped, so every ViewModel gets its own instance.
@Module
@InstallIn(SingletonComponent::class)
internal object DocumentsUseCaseModule {
    @Provides
    fun provideObserveDocumentsUseCase(repository: DocumentsRepository) = ObserveDocumentsUseCase {
        repository.observeDocuments().mapResult { documents -> documents.map(DocumentData::toDomain) }
    }

    @Provides
    fun provideLockWalletUseCase(appLock: AppLockRepository) = LockWalletUseCase { appLock.lockNow() }
}
