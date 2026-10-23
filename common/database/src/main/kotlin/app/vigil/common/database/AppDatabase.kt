package app.vigil.common.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import app.vigil.common.database.home.HomeEntryDao
import app.vigil.common.database.home.HomeEntryEntity

/**
 * The app's only database. One package per feature; one version number for the whole database.
 * Bump [version] with every schema change and add an AutoMigration (or a Migration_<from>_<to>)
 * plus its MigrationTestHelper test (skills/add-room-entity).
 */
@Database(
    entities = [HomeEntryEntity::class],
    version = 1,
)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun homeEntryDao(): HomeEntryDao
}
