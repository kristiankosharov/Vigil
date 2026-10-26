package app.vigil.feature.settings.ui.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class AccountScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks()
    }

    @Test
    fun appearanceShowsTheCurrentThemeAndOpens() {
        var opened = 0
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AccountScreen(state = AccountUiState(themeMode = ThemeMode.Dark), onOpenAppearance = { opened++ })
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.settings_theme_dark)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_appearance_title)).performClick()

        assertEquals(1, opened)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
