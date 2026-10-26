package app.vigil.feature.documents.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.documents.domain.model.DocumentFilter
import app.vigil.feature.documents.domain.model.DocumentStatus
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val actions = mutableListOf<DocumentsAction>()
    private val opened = mutableListOf<String>()

    private val rental = DocumentItem("1", "Rental agreement", DocumentStatus.ToSign, pageCount = 4, addedAt = 0L)
    private val contract = DocumentItem("2", "Employment contract", DocumentStatus.Signed, pageCount = 6, addedAt = 0L)

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks()
    }

    private fun state(
        documents: List<DocumentItem> = listOf(rental, contract),
        hasDocuments: Boolean = documents.isNotEmpty(),
        isSearching: Boolean = false,
        query: String = "",
        filter: DocumentFilter = DocumentFilter.All,
    ) = DocumentsUiState(
        isLoading = false,
        filter = filter,
        isSearching = isSearching,
        query = query,
        documents = documents.toImmutableList(),
        hasDocuments = hasDocuments,
        effect = null,
    )

    private fun show(state: DocumentsUiState) = composeTestRule.setContent {
        AppTheme(dynamicColor = false) {
            DocumentsScreen(state = state, onAction = { actions += it }, onOpenDocument = { opened += it })
        }
    }

    @Test
    fun documentsAreListedWithTheirStatusAndOpenOnTap() {
        show(state())

        composeTestRule.onNodeWithText("Employment contract").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.documents_status_to_sign),
            useUnmergedTree = true,
        )
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Rental agreement").performClick()

        assertEquals(listOf("1"), opened)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun chipsSearchAndLockSendActions() {
        show(state(filter = DocumentFilter.All))

        composeTestRule.onNodeWithText(context.getString(R.string.documents_filter_all)).assertIsSelected()
        composeTestRule.onNodeWithText(context.getString(R.string.documents_filter_signed)).performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.documents_search)).performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.documents_lock_now)).performClick()

        assertEquals(
            listOf(
                DocumentsAction.FilterSelected(DocumentFilter.Signed),
                DocumentsAction.SearchOpened,
                DocumentsAction.LockNow,
            ),
            actions,
        )
    }

    @Test
    fun searchSendsTheQueryAndCloses() {
        // The query is hoisted like in the ViewModel; a fixed "" would make the field resync and send "".
        composeTestRule.setContent {
            var state by remember { mutableStateOf(state(isSearching = true)) }
            AppTheme(dynamicColor = false) {
                DocumentsScreen(
                    state = state,
                    onAction = {
                        actions += it
                        if (it is DocumentsAction.QueryChanged) state = state.copy(query = it.query)
                    },
                    onOpenDocument = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.documents_search),
        ).performTextInput("lease")
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.documents_close_search)).performClick()

        assertEquals(listOf(DocumentsAction.QueryChanged("lease"), DocumentsAction.SearchClosed), actions)
    }

    @Test
    fun anEmptyWalletSaysSo() {
        show(state(documents = persistentListOf()))

        composeTestRule.onNodeWithText(context.getString(R.string.documents_empty_title)).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aSearchWithoutResultsNamesTheQuery() {
        show(state(documents = persistentListOf(), hasDocuments = true, isSearching = true, query = "insurance"))

        composeTestRule.onNodeWithText(
            context.getString(R.string.documents_no_match_title, "insurance"),
        ).assertIsDisplayed()
    }
}
