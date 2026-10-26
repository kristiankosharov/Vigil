package app.vigil.feature.settings.domain.di

import app.vigil.common.core.mapResult
import app.vigil.feature.settings.data.SettingsRepository
import app.vigil.feature.settings.domain.mapper.toData
import app.vigil.feature.settings.domain.mapper.toDomain
import app.vigil.feature.settings.domain.usecase.ObserveThemeModeUseCase
import app.vigil.feature.settings.domain.usecase.SetThemeModeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Pure Kotlin module (hilt-core only): SingletonComponent, unscoped, so every ViewModel gets its own instance.
@Module
@InstallIn(SingletonComponent::class)
internal object SettingsUseCaseModule {
    @Provides
    fun provideObserveThemeModeUseCase(repository: SettingsRepository) = ObserveThemeModeUseCase {
        repository.observeThemeMode().mapResult { it.toDomain() }
    }

    @Provides
    fun provideSetThemeModeUseCase(repository: SettingsRepository) = SetThemeModeUseCase { mode ->
        repository.setThemeMode(mode.toData())
    }
}
