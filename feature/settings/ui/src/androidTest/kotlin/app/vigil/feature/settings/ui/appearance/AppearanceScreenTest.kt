package app.vigil.feature.settings.ui.appearance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.settings.domain.model.ThemeMode
import app.vigil.feature.settings.ui.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val actions = mutableListOf<AppearanceAction>()
    private var backClicks = 0

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks()
    }

    private fun show(selected: ThemeMode?) = composeTestRule.setContent {
        AppTheme(dynamicColor = false) {
            AppearanceScreen(
                state = AppearanceUiState(selected = selected, effect = null),
                onAction = { actions += it },
                onBack = { backClicks++ },
            )
        }
    }

    // Each option row merges its texts and carries the selected state.
    private fun option(id: Int) = composeTestRule.onNodeWithText(context.getString(id))

    @Test
    fun theStoredChoiceIsSelected() {
        show(ThemeMode.Dark)

        option(R.string.settings_theme_dark).assertIsSelected()
        option(R.string.settings_theme_light).assertIsNotSelected()
        option(R.string.settings_theme_system).assertIsNotSelected()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun tappingAnOptionSelectsIt() {
        show(ThemeMode.System)

        option(R.string.settings_theme_light).performClick()

        assertEquals(listOf<AppearanceAction>(AppearanceAction.Select(ThemeMode.Light)), actions)
    }

    @Test
    fun backNavigatesBack() {
        show(ThemeMode.System)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.settings_back)).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun theSecurityScreensNoteIsShown() {
        show(null)

        composeTestRule.onNodeWithText(
            context.getString(R.string.settings_appearance_security_note),
        ).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
