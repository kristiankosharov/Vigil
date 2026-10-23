package app.vigil.feature.auth.data.repository

import app.vigil.common.network.SessionEvents
import app.vigil.feature.auth.data.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class SessionRepositoryImpl @Inject constructor(private val sessionEvents: SessionEvents) :
    SessionRepository {
    override fun observeSessionExpired(): Flow<Unit> = sessionEvents.expired
}
