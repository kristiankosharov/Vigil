package app.vigil.di

import app.vigil.BuildConfig
import app.vigil.common.network.BaseUrl
import app.vigil.common.network.IsDebugBuild
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The only bridge from BuildConfig to the rest of the app. */
@Module
@InstallIn(SingletonComponent::class)
object EnvironmentModule {
    @Provides
    @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    @Provides
    @IsDebugBuild
    fun provideIsDebugBuild(): Boolean = BuildConfig.DEBUG
}
