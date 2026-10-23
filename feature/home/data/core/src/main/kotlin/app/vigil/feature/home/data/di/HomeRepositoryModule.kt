package app.vigil.feature.home.data.di

import app.vigil.feature.home.data.HomeRepository
import app.vigil.feature.home.data.repository.HomeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface HomeRepositoryModule {
    @Binds
    fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
