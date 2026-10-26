package app.vigil.common.database.migrations

import androidx.room3.DeleteTable
import androidx.room3.migration.AutoMigrationSpec

/** 1 → 2: the placeholder Home table made way for documents (a new table, created by Room). */
@DeleteTable(tableName = "home_entry")
internal class DropHomeEntry : AutoMigrationSpec
