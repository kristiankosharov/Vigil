package app.vigil.feature.documents.domain.mapper

import app.vigil.feature.documents.data.DocumentData
import app.vigil.feature.documents.data.DocumentStatusData
import app.vigil.feature.documents.domain.model.Document
import app.vigil.feature.documents.domain.model.DocumentStatus

internal fun DocumentData.toDomain() = Document(
    id = id,
    title = title,
    status = when (status) {
        DocumentStatusData.ToSign -> DocumentStatus.ToSign
        DocumentStatusData.Signed -> DocumentStatus.Signed
        DocumentStatusData.Draft -> DocumentStatus.Draft
    },
    pageCount = pageCount,
    addedAt = addedAt,
    searchText = searchText,
)
