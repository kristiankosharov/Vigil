package app.vigil.common.network.di

import app.vigil.common.network.SessionEvents
import app.vigil.common.network.SessionEventsImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SessionEventsModule {
    @Binds
    fun bindSessionEvents(impl: SessionEventsImpl): SessionEvents
}
