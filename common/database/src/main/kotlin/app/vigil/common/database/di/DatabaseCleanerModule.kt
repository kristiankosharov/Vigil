package app.vigil.common.database.di

import app.vigil.common.core.UserDataCleaner
import app.vigil.common.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** On logout, every table is cleared: the database holds only user data. */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseCleanerModule {
    @Provides
    @IntoSet
    fun provideDatabaseCleaner(database: AppDatabase): UserDataCleaner = UserDataCleaner { database.clearAllTables() }
}
