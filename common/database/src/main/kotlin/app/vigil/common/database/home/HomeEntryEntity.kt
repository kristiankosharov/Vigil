package app.vigil.common.database.home

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** An entry shown on the Home screen. Used only by :feature:home:data:core. */
@Entity(tableName = "home_entry")
data class HomeEntryEntity(@PrimaryKey val id: String, val title: String)
