package app.vigil.feature.documents.data.mapper

import app.vigil.common.database.documents.DocumentEntity
import app.vigil.feature.documents.data.DocumentData
import app.vigil.feature.documents.data.DocumentStatusData

// The status column holds stable names, so renaming the enum never changes stored data.
private val StatusColumn = mapOf(
    DocumentStatusData.ToSign to "to_sign",
    DocumentStatusData.Signed to "signed",
    DocumentStatusData.Draft to "draft",
)

internal fun DocumentStatusData.toColumn(): String = StatusColumn.getValue(this)

/** An unknown status (a newer app version wrote it) reads as a draft: shown, but not signable. */
internal fun String.toDocumentStatus(): DocumentStatusData =
    StatusColumn.entries.firstOrNull { it.value == this }?.key ?: DocumentStatusData.Draft

internal fun DocumentEntity.toData() = DocumentData(
    id = id,
    title = title,
    status = status.toDocumentStatus(),
    pageCount = pageCount,
    addedAt = addedAt,
    searchText = searchText,
)
