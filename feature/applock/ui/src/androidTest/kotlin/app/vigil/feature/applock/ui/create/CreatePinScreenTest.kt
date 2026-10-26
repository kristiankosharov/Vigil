package app.vigil.feature.applock.ui.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private fun state(step: CreatePinStep, inputLength: Int = 0, match: PinMatch = PinMatch.Unknown) = CreatePinUiState(
        step = step,
        stepCount = 3,
        inputLength = inputLength,
        match = match,
        isPinWeak = false,
        isSaving = false,
        biometricRequest = null,
        effect = null,
        navigation = null,
    )

    private fun setScreen(
        state: CreatePinUiState,
        onAction: (CreatePinAction) -> Unit = {},
        onBackToSignIn: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CreatePinScreen(state = state, onAction = onAction, onPinCreated = {}, onBackToSignIn = onBackToSignIn)
        }
    }

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks()
    }

    @Test
    fun enterStepNeedsAllDigitsBeforeContinue() {
        setScreen(state(CreatePinStep.Enter, inputLength = 5))

        composeTestRule.onNodeWithText(context.getString(R.string.applock_create_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_continue)).assertIsNotEnabled()
    }

    @Test
    fun backOnTheFirstStepLeavesOnboarding() {
        var left = false
        val actions = mutableListOf<CreatePinAction>()
        setScreen(state(CreatePinStep.Enter), onAction = { actions += it }, onBackToSignIn = { left = true })

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.applock_back)).performClick()

        assertEquals(true, left)
        assertEquals(emptyList<CreatePinAction>(), actions)
    }

    @Test
    fun confirmStepSaysWhenThePinsMatch() {
        val actions = mutableListOf<CreatePinAction>()
        setScreen(state(CreatePinStep.Confirm, inputLength = 6, match = PinMatch.Match), onAction = { actions += it })

        composeTestRule.onNodeWithText(
            context.getString(R.string.applock_pins_match),
        ).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_continue)).assertIsEnabled().performClick()

        assertEquals(listOf<CreatePinAction>(CreatePinAction.Continue), actions)
    }

    @Test
    fun confirmStepSaysWhenThePinsDiffer() {
        setScreen(state(CreatePinStep.Confirm, inputLength = 6, match = PinMatch.Mismatch))

        composeTestRule.onNodeWithText(
            context.getString(R.string.applock_pins_mismatch),
        ).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_continue)).assertIsNotEnabled()
    }

    @Test
    fun biometricStepOffersEnableAndNotNow() {
        val actions = mutableListOf<CreatePinAction>()
        setScreen(state(CreatePinStep.Biometric), onAction = { actions += it })

        composeTestRule.onNodeWithText(context.getString(R.string.applock_biometric_setup_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_biometric_enable)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_biometric_not_now)).performClick()

        assertEquals(listOf(CreatePinAction.EnableBiometric, CreatePinAction.SkipBiometric), actions)
    }
}
