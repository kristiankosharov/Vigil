package app.vigil.feature.settings.data.di

import app.vigil.feature.settings.data.SettingsRepository
import app.vigil.feature.settings.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsRepositoryModule {
    @Binds
    fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
