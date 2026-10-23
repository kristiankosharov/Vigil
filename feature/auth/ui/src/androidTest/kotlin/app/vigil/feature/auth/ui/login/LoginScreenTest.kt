package app.vigil.feature.auth.ui.login

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.auth.ui.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks()
    }

    @Test
    fun tappingLogInSendsSubmit() {
        val actions = mutableListOf<LoginAction>()
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                LoginScreen(
                    state = LoginUiState(isLoggingIn = false, effect = null, navigation = null),
                    onAction = { actions += it },
                    onLoggedIn = {},
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.auth_login_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.auth_login_button)).performClick()

        assertEquals(listOf<LoginAction>(LoginAction.Submit), actions)
    }

    @Test
    fun loggedInNavigationCallsOnLoggedInOnce() {
        var loggedIn = 0
        val actions = mutableListOf<LoginAction>()
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                LoginScreen(
                    state = LoginUiState(isLoggingIn = false, effect = null, navigation = LoginNavigation.LoggedIn),
                    onAction = { actions += it },
                    onLoggedIn = { loggedIn++ },
                )
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(1, loggedIn)
        assertEquals(listOf<LoginAction>(LoginAction.NavigationConsumed(LoginNavigation.LoggedIn)), actions)
    }

    @Test
    fun whileLoggingInTheButtonIsDisabledAndShowsProgress() {
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                LoginScreen(
                    state = LoginUiState(isLoggingIn = true, effect = null, navigation = null),
                    onAction = {},
                    onLoggedIn = {},
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.auth_login_button)).assertDoesNotExist()
        composeTestRule.onNode(hasClickAction())
            .assertIsNotEnabled()
            .assertContentDescriptionContains(context.getString(R.string.auth_login_in_progress))
    }
}
