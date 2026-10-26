package app.vigil.feature.auth.data.di

import android.app.Application
import app.vigil.feature.auth.data.remote.CurrentActivityTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** `App` registers every `ActivityLifecycleCallbacks` in this set. */
@Module
@InstallIn(SingletonComponent::class)
internal interface AuthActivityModule {
    @Binds
    @IntoSet
    fun bindCurrentActivityTracker(tracker: CurrentActivityTracker): Application.ActivityLifecycleCallbacks
}
