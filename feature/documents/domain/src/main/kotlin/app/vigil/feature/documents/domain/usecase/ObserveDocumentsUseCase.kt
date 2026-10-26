package app.vigil.feature.documents.domain.usecase

import app.vigil.common.core.AppResult
import app.vigil.feature.documents.domain.model.Document
import kotlinx.coroutines.flow.Flow

/** Every document in the wallet, newest first. */
public fun interface ObserveDocumentsUseCase {
    public operator fun invoke(): Flow<AppResult<List<Document>>>
}
