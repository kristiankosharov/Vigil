package app.vigil.feature.applock.ui.lock

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun state(status: AppLockStatus, pin: String = "") = AppLockUiState(
        status = status,
        pin = pin,
        isPinWrong = false,
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
    fun padKeysAndUnlockSendActions() {
        val actions = mutableListOf<AppLockAction>()
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(state = state(AppLockStatus.Locked, pin = "1234"), onAction = { actions += it }) {}
            }
        }

        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.applock_delete_digit)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.applock_unlock)).performClick()

        assertEquals(
            listOf(AppLockAction.PinChanged("12345"), AppLockAction.PinChanged("123"), AppLockAction.SubmitPin),
            actions,
        )
    }

    @Test
    fun thePinIsReadOutAsACountOnly() {
        composeTestRule.setContent {
            AppTheme(dynamicColor = false) {
                AppLockLayer(state = state(AppLockStatus.Locked, pin = "1234"), onAction = {}) {}
            }
        }

        val progress = context.resources.getQuantityString(R.plurals.applock_pin_progress, 4, 4)
        composeTestRule.onNodeWithContentDescription(progress).assertIsDisplayed()
        composeTestRule.onNodeWithText("1234").assertDoesNotExist()
    }
}
