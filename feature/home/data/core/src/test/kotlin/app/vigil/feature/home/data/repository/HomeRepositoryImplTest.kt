package app.vigil.feature.home.data.repository

import android.database.SQLException
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.database.home.HomeEntryDao
import app.vigil.common.database.home.HomeEntryEntity
import app.vigil.feature.home.data.HomeEntryData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class HomeRepositoryImplTest : FunSpec({

    test("observeEntries emits the stored entries as data models") {
        val dao = mockk<HomeEntryDao> {
            every { observeAll() } returns flowOf(listOf(HomeEntryEntity(id = "1", title = "First")))
        }

        HomeRepositoryImpl(dao).observeEntries().test {
            awaitItem() shouldBe AppResult.Success(listOf(HomeEntryData(id = "1", title = "First")))
            awaitComplete()
        }
    }

    test("a database failure is emitted as AppError.Local") {
        val dao = mockk<HomeEntryDao> {
            every { observeAll() } returns flow { throw mockk<SQLException>(relaxed = true) }
        }

        HomeRepositoryImpl(dao).observeEntries().test {
            awaitItem().shouldBeInstanceOf<AppResult.Failure>().error.shouldBeInstanceOf<AppError.Local>()
            cancelAndIgnoreRemainingEvents()
        }
    }
})
