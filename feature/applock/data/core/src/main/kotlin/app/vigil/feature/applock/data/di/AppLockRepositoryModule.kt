package app.vigil.feature.applock.data.di

import app.vigil.feature.applock.data.AppLockRepository
import app.vigil.feature.applock.data.repository.AppLockRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AppLockRepositoryModule {
    @Binds
    fun bindAppLockRepository(impl: AppLockRepositoryImpl): AppLockRepository
}
