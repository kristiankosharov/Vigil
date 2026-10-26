package app.vigil.feature.documents.domain.model

public data class Document(
    val id: String,
    val title: String,
    val status: DocumentStatus,
    val pageCount: Int,
    /** Epoch milliseconds. */
    val addedAt: Long,
    /** Parties and text found in the scan. */
    val searchText: String,
) {
    /**
     * On-device search: every word of [query] appears in the title or the scanned text, ignoring
     * case. A blank query matches everything.
     */
    public fun matches(query: String): Boolean {
        val words = query.trim().lowercase().split(Whitespace).filter(String::isNotEmpty)
        val haystack = "$title $searchText".lowercase()
        return words.all(haystack::contains)
    }

    private companion object {
        val Whitespace = Regex("\\s+")
    }
}

public enum class DocumentStatus { ToSign, Signed, Draft }

/** The filter chips of the Documents tab. */
public enum class DocumentFilter {
    All,
    ToSign,
    Signed,
    Drafts,
    ;

    public fun accepts(status: DocumentStatus): Boolean = when (this) {
        All -> true
        ToSign -> status == DocumentStatus.ToSign
        Signed -> status == DocumentStatus.Signed
        Drafts -> status == DocumentStatus.Draft
    }
}
