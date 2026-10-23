package app.vigil.feature.home.data

import app.vigil.common.core.AppResult
import kotlinx.coroutines.flow.Flow

public interface HomeRepository {
    /** The entries stored on this device (Room is the source of truth). */
    public fun observeEntries(): Flow<AppResult<List<HomeEntryData>>>

    // TODO(home): add `suspend fun refreshEntries(): AppResult<Unit>` that loads the entries from the
    //  API into Room once the backend exists.
}

public data class HomeEntryData(val id: String, val title: String)
