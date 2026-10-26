package app.vigil.feature.documents.data.repository

import app.vigil.common.core.AppResult
import app.vigil.common.database.asDbResult
import app.vigil.common.database.documents.DocumentDao
import app.vigil.common.database.documents.DocumentEntity
import app.vigil.feature.documents.data.DocumentData
import app.vigil.feature.documents.data.DocumentsRepository
import app.vigil.feature.documents.data.mapper.toData
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DocumentsRepositoryImpl @Inject constructor(private val dao: DocumentDao) : DocumentsRepository {

    override fun observeDocuments(): Flow<AppResult<List<DocumentData>>> = dao.observeAll()
        .map { entities -> entities.map(DocumentEntity::toData) }
        .asDbResult()
}
