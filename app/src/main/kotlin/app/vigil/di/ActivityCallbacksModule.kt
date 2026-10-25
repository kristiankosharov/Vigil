package app.vigil.di

import android.app.Application
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Declares the set `App` registers, so it exists even when no feature contributes to it. */
@Module
@InstallIn(SingletonComponent::class)
interface ActivityCallbacksModule {
    @Multibinds
    fun activityCallbacks(): Set<Application.ActivityLifecycleCallbacks>
}
