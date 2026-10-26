package app.vigil.common.database.documents

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A document in the wallet. Used only by :feature:documents:data:core. */
@Entity(tableName = "documents_document")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** `to_sign`, `signed` or `draft`; mapped to an enum in the repository. */
    val status: String,
    val pageCount: Int,
    /** When the document was added, in epoch milliseconds. */
    val addedAt: Long,
    /** Parties and text found in the scan, for on-device search. */
    val searchText: String,
)
