package app.vigil.feature.documents.data.repository

import androidx.datastore.core.DataStore
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.datastore.asStoreResult
import app.vigil.common.datastore.safeStoreCall
import app.vigil.common.security.Vault
import app.vigil.feature.documents.data.DocumentData
import app.vigil.feature.documents.data.DocumentStatusData
import app.vigil.feature.documents.data.DocumentsRepository
import app.vigil.feature.documents.data.NewDocumentData
import app.vigil.feature.documents.data.local.DocumentCrypto
import app.vigil.feature.documents.data.local.DocumentFiles
import app.vigil.feature.documents.data.local.DocumentMetadata
import app.vigil.feature.documents.data.local.DocumentsIndex
import app.vigil.feature.documents.data.local.StoredDocument
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform

internal class DocumentsRepositoryImpl @Inject constructor(
    private val store: DataStore<DocumentsIndex>,
    private val files: DocumentFiles,
    private val crypto: DocumentCrypto,
    private val vault: Vault,
) : DocumentsRepository {

    override fun observeDocuments(): Flow<AppResult<List<DocumentData>>> =
        combine(vault.isOpen, store.data) { open, index -> open to index }
            .transform { (open, index) ->
                if (open) emit(index.documents.map(::toData).sortedByDescending(DocumentData::addedAt))
            }
            .asStoreResult()

    override suspend fun addDocument(document: NewDocumentData): AppResult<String> = safeStoreCall {
        val id = UUID.randomUUID().toString()
        val key = crypto.newKey()
        var stored = false
        try {
            document.pages.forEachIndexed { index, page ->
                files.write(id, index, crypto.sealPage(key, id, index, page))
            }
            val metadata = DocumentMetadata(
                title = document.title,
                status = document.status.stableName(),
                pageCount = document.pages.size,
                addedAt = document.addedAt,
                searchText = document.searchText,
            )
            val entry = StoredDocument(
                id = id,
                keyAlias = DocumentCrypto.KEY_ALIAS,
                wrappedKey = crypto.wrapKey(id, key),
                sealedMetadata = crypto.sealMetadata(id, metadata),
            )
            store.updateData { it.copy(documents = it.documents + entry) }
            stored = true
            id
        } finally {
            key.fill(0)
            if (!stored) files.delete(id) // no half-saved pages without an entry
        }
    }

    override suspend fun readPage(id: String, index: Int): AppResult<ByteArray> {
        val result = safeStoreCall {
            val document = store.data.first().documents.firstOrNull { it.id == id }
            if (document == null || index !in 0 until crypto.openMetadata(document).pageCount) {
                null
            } else {
                val key = crypto.unwrapKey(document)
                try {
                    crypto.openPage(key, id, index, files.read(id, index))
                } finally {
                    key.fill(0)
                }
            }
        }
        return when (result) {
            is AppResult.Success -> result.data?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Local.NotFound)

            is AppResult.Failure -> result
        }
    }

    override suspend fun deleteDocument(id: String): AppResult<Unit> = safeStoreCall {
        // The wrapped key goes first: from then on the pages are unreadable even if deleting them fails.
        store.updateData { index -> index.copy(documents = index.documents.filterNot { it.id == id }) }
        files.delete(id)
    }

    private fun toData(document: StoredDocument): DocumentData {
        val metadata = crypto.openMetadata(document)
        return DocumentData(
            id = document.id,
            title = metadata.title,
            status = metadata.status.toDocumentStatus(),
            pageCount = metadata.pageCount,
            addedAt = metadata.addedAt,
            searchText = metadata.searchText,
        )
    }
}

// Stable names in the store, so renaming the enum never changes stored data.
private val StatusNames = mapOf(
    DocumentStatusData.ToSign to "to_sign",
    DocumentStatusData.Signed to "signed",
    DocumentStatusData.Draft to "draft",
)

private fun DocumentStatusData.stableName(): String = StatusNames.getValue(this)

/** An unknown status (a newer app version wrote it) reads as a draft: shown, but not signable. */
private fun String.toDocumentStatus(): DocumentStatusData =
    StatusNames.entries.firstOrNull { it.value == this }?.key ?: DocumentStatusData.Draft
