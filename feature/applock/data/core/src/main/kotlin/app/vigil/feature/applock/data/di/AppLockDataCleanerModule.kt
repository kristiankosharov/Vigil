package app.vigil.feature.applock.data.di

import androidx.datastore.core.DataStore
import app.vigil.common.core.UserDataCleaner
import app.vigil.feature.applock.data.local.AppLockSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** On logout the PIN goes with the account: the next user creates their own. */
@Module
@InstallIn(SingletonComponent::class)
internal object AppLockDataCleanerModule {
    @Provides
    @IntoSet
    fun provideAppLockDataCleaner(store: DataStore<AppLockSettings>): UserDataCleaner =
        UserDataCleaner { store.updateData { AppLockSettings() } }
}
