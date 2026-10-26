package app.vigil.feature.documents.data.repository

import android.database.SQLException
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.database.documents.DocumentDao
import app.vigil.common.database.documents.DocumentEntity
import app.vigil.feature.documents.data.DocumentData
import app.vigil.feature.documents.data.DocumentStatusData
import app.vigil.feature.documents.data.mapper.toColumn
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class DocumentsRepositoryImplTest : FunSpec({

    fun entity(status: String) = DocumentEntity(
        id = "1",
        title = "Rental agreement",
        status = status,
        pageCount = 4,
        addedAt = 1_000L,
        searchText = "landlord",
    )

    test("observeDocuments emits the stored documents as data models") {
        val dao = mockk<DocumentDao> { every { observeAll() } returns flowOf(listOf(entity("to_sign"))) }

        DocumentsRepositoryImpl(dao).observeDocuments().test {
            awaitItem() shouldBe AppResult.Success(
                listOf(DocumentData("1", "Rental agreement", DocumentStatusData.ToSign, 4, 1_000L, "landlord")),
            )
            awaitComplete()
        }
    }

    test("every status round-trips through its column value; an unknown one reads as a draft") {
        DocumentStatusData.entries.forEach { status ->
            val dao = mockk<DocumentDao> { every { observeAll() } returns flowOf(listOf(entity(status.toColumn()))) }
            DocumentsRepositoryImpl(dao).observeDocuments().test {
                val documents = awaitItem().shouldBeInstanceOf<AppResult.Success<List<DocumentData>>>().data
                documents.single().status shouldBe status
                awaitComplete()
            }
        }

        val dao = mockk<DocumentDao> { every { observeAll() } returns flowOf(listOf(entity("archived"))) }
        DocumentsRepositoryImpl(dao).observeDocuments().test {
            awaitItem().shouldBeInstanceOf<AppResult.Success<List<DocumentData>>>().data.single().status shouldBe
                DocumentStatusData.Draft
            awaitComplete()
        }
    }

    test("a database failure is emitted as AppError.Local") {
        val dao = mockk<DocumentDao> {
            every { observeAll() } returns flow { throw mockk<SQLException>(relaxed = true) }
        }

        DocumentsRepositoryImpl(dao).observeDocuments().test {
            awaitItem().shouldBeInstanceOf<AppResult.Failure>().error.shouldBeInstanceOf<AppError.Local>()
            cancelAndIgnoreRemainingEvents()
        }
    }
})
