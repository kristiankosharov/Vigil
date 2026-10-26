package app.vigil.feature.applock.ui.lock

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class AppLockLayerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun state(status: AppLockStatus, pinLength: Int = 0) = AppLockUiState(
        status = status,
        pinLength = pinLength,
        isPinWrong = false,
        attemptsLeft = null,
        retrySeconds = 0,
        isChecking = false,
        isBiometricEnabled = false,
        biometricRequest = null,
        effect = null,
    )

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks()
    }

    @Test
    fun unlockedShowsTheApp() {
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(state = state(AppLockStatus.Unlocked), onAction = {}) { Text("Secret content") }
            }
        }

        composeTestRule.onNodeWithText("Secret content").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_locked_title)).assertDoesNotExist()
    }

    @Test
    fun lockedHidesTheAppAndAsksForThePin() {
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(state = state(AppLockStatus.Locked), onAction = {}) { Text("Secret content") }
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.applock_locked_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Secret content").assertDoesNotExist() // not in the semantics tree
    }

    @Test
    fun loadingKeepsTheAppCovered() {
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(state = state(AppLockStatus.Loading), onAction = {}) { Text("Secret content") }
            }
        }

        composeTestRule.onNodeWithText("Secret content").assertDoesNotExist()
    }

    @Test
    fun padKeysSendActions() {
        val actions = mutableListOf<AppLockAction>()
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(state = state(AppLockStatus.Locked, pinLength = 4), onAction = { actions += it }) {}
            }
        }

        composeTestRule.onNodeWithText("5").performScrollTo().performClick()
        // Below the dots on small screens: scroll first, or the tap lands off-screen.
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.applock_delete_digit))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(AppLockAction.Digit(5), AppLockAction.Delete), actions)
    }

    @Test
    fun fingerprintKeyShowsOnlyWithBiometricsEnabled() {
        val actions = mutableListOf<AppLockAction>()
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(
                    state = state(AppLockStatus.Locked).copy(isBiometricEnabled = true),
                    onAction = { actions += it },
                ) {}
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.applock_use_biometric))
            .performScrollTo()
            .performClick()

        assertEquals(listOf<AppLockAction>(AppLockAction.UseBiometric), actions)
    }

    @Test
    fun thePinIsReadOutAsACountOnly() {
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(state = state(AppLockStatus.Locked, pinLength = 4), onAction = {}) {}
            }
        }

        val progress = context.resources.getQuantityString(R.plurals.applock_pin_progress, 4, 4, 6)
        composeTestRule.onNodeWithContentDescription(progress).assertIsDisplayed()
        composeTestRule.onNodeWithText("1234").assertDoesNotExist()
    }

    @Test
    fun afterTooManyWrongPinsTheWaitShowsAndTheKeysAreOff() {
        val actions = mutableListOf<AppLockAction>()
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(
                    state = state(AppLockStatus.Locked).copy(isPinWrong = true, attemptsLeft = 5, retrySeconds = 75),
                    onAction = { actions += it },
                ) {}
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.applock_retry_in, "1:15")).assertIsDisplayed()
        composeTestRule.onNodeWithText("5").performScrollTo().performClick()

        assertEquals(emptyList<AppLockAction>(), actions)
    }
}
