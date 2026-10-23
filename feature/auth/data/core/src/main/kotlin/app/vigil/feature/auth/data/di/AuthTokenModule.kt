package app.vigil.feature.auth.data.di

import app.vigil.common.network.TokenRefresher
import app.vigil.common.network.TokenStorage
import app.vigil.feature.auth.data.local.TokenStorageImpl
import app.vigil.feature.auth.data.remote.TokenRefresherImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** What :common:network's Auth plugin needs from this feature. */
@Module
@InstallIn(SingletonComponent::class)
internal interface AuthTokenModule {
    @Binds
    fun bindTokenStorage(impl: TokenStorageImpl): TokenStorage

    @Binds
    fun bindTokenRefresher(impl: TokenRefresherImpl): TokenRefresher
}
