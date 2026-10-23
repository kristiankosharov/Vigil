package app.vigil.common.database.di

import app.vigil.common.database.AppDatabase
import app.vigil.common.database.home.HomeEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Every DAO, so features inject their DAO and never see AppDatabase. */
@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {
    @Provides
    fun provideHomeEntryDao(database: AppDatabase): HomeEntryDao = database.homeEntryDao()
}
