package app.vigil.feature.auth.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.auth.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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

    private fun state(
        method: LoginMethod = LoginMethod.Email,
        phoneStep: PhoneStep = PhoneStep.EnterNumber,
        oidcProviders: ImmutableList<OidcProviderItem> = persistentListOf(),
        error: UiText? = null,
        isBusy: Boolean = false,
        navigation: LoginNavigation? = null,
    ) = LoginUiState(
        method = method,
        emailMode = EmailMode.SignIn,
        email = "",
        password = "",
        phoneStep = phoneStep,
        phoneNumber = "+359888123456",
        code = "",
        oidcProviders = oidcProviders,
        error = error,
        isBusy = isBusy,
        effect = null,
        navigation = navigation,
    )

    private fun setContent(state: LoginUiState, onAction: (LoginAction) -> Unit = {}, onLoggedIn: () -> Unit = {}) {
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                LoginScreen(state = state, onAction = onAction, onLoggedIn = onLoggedIn)
            }
        }
    }

    private fun text(id: Int) = context.getString(id)

    // "Email" is also the method selector's label: match only the text field.
    private fun field(labelId: Int) = composeTestRule.onNode(hasSetTextAction() and hasText(text(labelId)))

    @Test
    fun emailFormSendsTypedValuesAndSubmit() {
        val actions = mutableListOf<LoginAction>()
        // Keeps the typed text like the ViewModel would; a fixed state would reset the fields.
        composeTestRule.setContent {
            var current by remember { mutableStateOf(state()) }
            AppTheme(dynamicColor = false) {
                LoginScreen(
                    state = current,
                    onAction = { action ->
                        actions += action
                        current = when (action) {
                            is LoginAction.EmailChanged -> current.copy(email = action.email)
                            is LoginAction.PasswordChanged -> current.copy(password = action.password)
                            else -> current
                        }
                    },
                    onLoggedIn = {},
                )
            }
        }

        composeTestRule.onNodeWithText(text(R.string.auth_login_title)).assertIsDisplayed()
        field(R.string.auth_email_label).performTextInput("ana@example.com")
        field(R.string.auth_password_label).performTextInput("secret1")
        composeTestRule.onNodeWithText(text(R.string.auth_sign_in_button)).performScrollTo().performClick()

        assertEquals(
            listOf(
                LoginAction.EmailChanged("ana@example.com"),
                LoginAction.PasswordChanged("secret1"),
                LoginAction.SubmitEmail,
            ),
            actions,
        )
    }

    @Test
    fun theErrorIsShown() {
        setContent(state(error = UiText.Resource(R.string.auth_error_invalid_credentials)))

        composeTestRule.onNodeWithText(text(R.string.auth_error_invalid_credentials)).assertIsDisplayed()
    }

    @Test
    fun phoneCodeStepVerifiesAndCanChangeTheNumber() {
        val actions = mutableListOf<LoginAction>()
        setContent(state(method = LoginMethod.Phone, phoneStep = PhoneStep.EnterCode), onAction = { actions += it })

        composeTestRule.onNodeWithText(context.getString(R.string.auth_code_sent, "+359888123456")).assertIsDisplayed()
        composeTestRule.onNodeWithText(text(R.string.auth_verify_button)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(text(R.string.auth_change_number)).performScrollTo().performClick()

        assertEquals(listOf(LoginAction.VerifyCode, LoginAction.ChangePhoneNumber), actions)
    }

    @Test
    fun oidcButtonsAppearOnlyForConfiguredProviders() {
        val actions = mutableListOf<LoginAction>()
        setContent(
            state(oidcProviders = persistentListOf(OidcProviderItem("oidc.company", "Company SSO"))),
            onAction = { actions += it },
        )

        composeTestRule.onNodeWithText(context.getString(R.string.auth_oidc_button, "Company SSO"))
            .performScrollTo()
            .performClick()

        assertEquals(listOf<LoginAction>(LoginAction.OidcSelected("oidc.company")), actions)
    }

    @Test
    fun loggedInNavigationCallsOnLoggedInOnce() {
        var loggedIn = 0
        val actions = mutableListOf<LoginAction>()
        setContent(state(navigation = LoginNavigation.LoggedIn), onAction = {
            actions += it
        }, onLoggedIn = { loggedIn++ })
        composeTestRule.waitForIdle()

        assertEquals(1, loggedIn)
        assertEquals(listOf<LoginAction>(LoginAction.NavigationConsumed(LoginNavigation.LoggedIn)), actions)
    }

    @Test
    fun whileBusyTheButtonIsDisabledAndShowsProgress() {
        setContent(state(isBusy = true))

        composeTestRule.onNodeWithText(text(R.string.auth_sign_in_button)).assertDoesNotExist()
        composeTestRule.onNode(hasClickAction() and hasContentDescription(text(R.string.auth_in_progress)))
            .assertIsNotEnabled()
            .assertContentDescriptionContains(text(R.string.auth_in_progress))
    }
}
