package app.vigil.common.database.home

import androidx.room3.Dao
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeEntryDao {
    @Query("SELECT * FROM home_entry ORDER BY title")
    fun observeAll(): Flow<List<HomeEntryEntity>>

    // TODO(home): add the writes (@Upsert / replaceAll in a @Transaction) with the real API refresh.
}
