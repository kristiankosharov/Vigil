package app.vigil.feature.home.domain.di

import app.vigil.common.core.mapResult
import app.vigil.feature.home.data.HomeEntryData
import app.vigil.feature.home.data.HomeRepository
import app.vigil.feature.home.domain.mapper.toDomain
import app.vigil.feature.home.domain.usecase.ObserveHomeEntriesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Pure Kotlin module (hilt-core only): SingletonComponent, unscoped, so every ViewModel gets its own instance.
@Module
@InstallIn(SingletonComponent::class)
internal object HomeUseCaseModule {
    @Provides
    fun provideObserveHomeEntriesUseCase(repository: HomeRepository) = ObserveHomeEntriesUseCase {
        repository.observeEntries().mapResult { entries -> entries.map(HomeEntryData::toDomain) }
    }
}
