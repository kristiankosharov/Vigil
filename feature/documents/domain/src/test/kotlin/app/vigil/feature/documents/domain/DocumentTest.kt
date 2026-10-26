package app.vigil.feature.documents.domain

import app.vigil.feature.documents.domain.model.Document
import app.vigil.feature.documents.domain.model.DocumentFilter
import app.vigil.feature.documents.domain.model.DocumentStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DocumentTest : FunSpec({
    val rental = Document(
        id = "1",
        title = "Rental agreement",
        status = DocumentStatus.ToSign,
        pageCount = 4,
        addedAt = 0L,
        searchText = "Landlord rent lease",
    )

    test("a blank query matches everything") {
        rental.matches("") shouldBe true
        rental.matches("   ") shouldBe true
    }

    test("every word must appear in the title or the scanned text, ignoring case") {
        rental.matches("RENTAL") shouldBe true
        rental.matches("landlord agreement") shouldBe true
        rental.matches("landlord insurance") shouldBe false
    }

    test("each filter accepts only its status; All accepts every status") {
        DocumentStatus.entries.forEach { DocumentFilter.All.accepts(it) shouldBe true }
        DocumentFilter.ToSign.accepts(DocumentStatus.ToSign) shouldBe true
        DocumentFilter.ToSign.accepts(DocumentStatus.Signed) shouldBe false
        DocumentFilter.Signed.accepts(DocumentStatus.Signed) shouldBe true
        DocumentFilter.Drafts.accepts(DocumentStatus.Draft) shouldBe true
        DocumentFilter.Drafts.accepts(DocumentStatus.ToSign) shouldBe false
    }
})
