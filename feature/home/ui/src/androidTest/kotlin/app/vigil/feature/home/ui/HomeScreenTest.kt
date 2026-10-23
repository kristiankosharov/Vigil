package app.vigil.feature.home.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vigil.common.ui.theme.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks()
    }

    private fun show(state: HomeUiState) = composeTestRule.setContent {
        AppTheme(dynamicColor = false) { HomeScreen(state = state, onAction = {}) }
    }

    @Test
    fun loadingShowsTheProgressIndicator() {
        show(HomeUiState(isLoading = true, entries = persistentListOf(), effect = null))

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.home_loading)).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun noEntriesShowsTheEmptyState() {
        show(HomeUiState(isLoading = false, entries = persistentListOf(), effect = null))

        composeTestRule.onNodeWithText(context.getString(R.string.home_empty_title)).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun entriesAreListed() {
        show(HomeUiState(isLoading = false, entries = persistentListOf(HomeEntryItem("1", "First")), effect = null))

        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
