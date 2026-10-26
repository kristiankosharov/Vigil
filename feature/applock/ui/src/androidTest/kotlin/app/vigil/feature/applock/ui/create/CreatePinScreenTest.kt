package app.vigil.feature.applock.ui.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.applock.ui.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreatePinScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun state(step: CreatePinStep, input: String) = CreatePinUiState(
        step = step,
        input = input,
        isBiometricAvailable = true,
        isBiometricEnabled = true,
        error = null,
        isSaving = false,
        effect = null,
        navigation = null,
    )

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks()
    }

    @Test
    fun enterStepAsksForThePinAndNeedsFourDigits() {
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) { CreatePinScreen(state(CreatePinStep.Enter, "123"), {}, {}) }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.applock_create_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_next)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_enable_biometric)).assertDoesNotExist()
    }

    @Test
    fun repeatStepSavesAndOffersBiometrics() {
        val actions = mutableListOf<CreatePinAction>()
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                CreatePinScreen(state(CreatePinStep.Repeat, "1234"), onAction = { actions += it }, onPinCreated = {})
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.applock_repeat_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            context.getString(R.string.applock_enable_biometric),
        ).performScrollTo().assertIsDisplayed()
        // Below the pad on small screens: scroll first, or the tap lands off-screen.
        composeTestRule.onNodeWithText(context.getString(R.string.applock_save)).performScrollTo().performClick()

        assertEquals(listOf<CreatePinAction>(CreatePinAction.Submit), actions)
    }
}
