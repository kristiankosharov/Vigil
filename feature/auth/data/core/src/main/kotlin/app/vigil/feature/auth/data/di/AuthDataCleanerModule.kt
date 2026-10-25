package app.vigil.feature.auth.data.di

import androidx.datastore.core.DataStore
import app.vigil.common.core.UserDataCleaner
import app.vigil.feature.auth.data.local.AuthSession
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** On logout: sign out of Firebase and forget the backend tokens. */
@Module
@InstallIn(SingletonComponent::class)
internal object AuthDataCleanerModule {
    @Provides
    @IntoSet
    fun provideAuthDataCleaner(auth: FirebaseAuth, store: DataStore<AuthSession>): UserDataCleaner = UserDataCleaner {
        auth.signOut()
        store.updateData { AuthSession() }
    }
}
