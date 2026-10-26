package app.vigil.common.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import app.vigil.common.database.documents.DocumentDao
import app.vigil.common.database.documents.DocumentEntity
import app.vigil.common.database.migrations.DropHomeEntry

/**
 * The app's only database. One package per feature; one version number for the whole database.
 * Bump [version] with every schema change and add an AutoMigration (or a Migration_<from>_<to>)
 * plus its MigrationTestHelper test (skills/add-room-entity).
 */
@Database(
    entities = [DocumentEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = DropHomeEntry::class)],
)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
