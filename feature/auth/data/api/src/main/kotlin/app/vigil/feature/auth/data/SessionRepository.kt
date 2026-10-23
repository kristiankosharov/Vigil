package app.vigil.feature.auth.data

import kotlinx.coroutines.flow.Flow

public interface SessionRepository {
    /** Emits when the session ended on the server side (refresh token rejected or missing). */
    public fun observeSessionExpired(): Flow<Unit>
}
