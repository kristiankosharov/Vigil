package app.vigil.common.network.di

import app.vigil.common.network.Authenticated
import app.vigil.common.network.BaseUrl
import app.vigil.common.network.IsDebugBuild
import app.vigil.common.network.SessionEvents
import app.vigil.common.network.TokenRefresher
import app.vigil.common.network.TokenStorage
import app.vigil.common.network.Unauthenticated
import app.vigil.common.network.createHttpClient
import app.vigil.common.network.installBearerAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object HttpClientModule {
    @Provides
    @Singleton
    @Unauthenticated
    fun provideUnauthenticatedClient(@BaseUrl baseUrl: String, @IsDebugBuild isDebugBuild: Boolean): HttpClient =
        createHttpClient(baseUrl, isDebugBuild)

    @Provides
    @Singleton
    @Authenticated
    fun provideAuthenticatedClient(
        @BaseUrl baseUrl: String,
        @IsDebugBuild isDebugBuild: Boolean,
        tokenStorage: TokenStorage,
        tokenRefresher: TokenRefresher,
        sessionEvents: SessionEvents,
    ): HttpClient = createHttpClient(baseUrl, isDebugBuild) {
        installBearerAuth(tokenStorage, tokenRefresher, sessionEvents)
    }
}
