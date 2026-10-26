package app.vigil.feature.applock.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.designsystem.PrimaryButton
import app.vigil.common.ui.mvi.NavigationHandler
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.theme.SecurityTheme
import app.vigil.common.ui.theme.statusColors
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.ui.R
import app.vigil.feature.applock.ui.biometric.BiometricPromptHandler
import app.vigil.feature.applock.ui.components.PinDots
import app.vigil.feature.applock.ui.components.PinDotsStatus
import app.vigil.feature.applock.ui.components.PinPad

/**
 * Onboarding right after sign-in: create PIN, confirm it, enable fingerprint. [onBackToSignIn] is
 * the back arrow (or system Back) on the first step.
 */
@Composable
fun CreatePinRoute(onPinCreated: () -> Unit, onBackToSignIn: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: CreatePinViewModel = hiltViewModel()
    CreatePinScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onPinCreated = onPinCreated,
        onBackToSignIn = onBackToSignIn,
        modifier = modifier,
    )
}

@Composable
internal fun CreatePinScreen(
    state: CreatePinUiState,
    onAction: (CreatePinAction) -> Unit,
    onPinCreated: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val onBack = { if (state.step == CreatePinStep.Enter) onBackToSignIn() else onAction(CreatePinAction.Back) }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { onAction(CreatePinAction.EffectConsumed(it)) },
    )
    NavigationHandler(
        navigation = state.navigation,
        onNavigate = { navigation ->
            when (navigation) {
                CreatePinNavigation.PinCreated -> onPinCreated()
            }
        },
        onNavigationConsumed = { onAction(CreatePinAction.NavigationConsumed(it)) },
    )
    BiometricPromptHandler(
        request = state.biometricRequest,
        title = stringResource(R.string.applock_biometric_confirm_title),
        negativeText = stringResource(R.string.applock_cancel),
        onSucceeded = { onAction(CreatePinAction.BiometricSucceeded) },
        onRequestConsumed = { onAction(CreatePinAction.BiometricRequestConsumed(it)) },
    )
    BackHandler(onBack = onBack)

    SecurityTheme {
        Scaffold(
            modifier = modifier,
            topBar = {
                OnboardingTopBar(step = state.step.ordinal + 1, stepCount = state.stepCount, onBack = onBack)
            },
            bottomBar = { OnboardingButtons(state = state, onAction = onAction) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()) // 200% font scale on small screens
                .padding(horizontal = 32.dp)
            when (state.step) {
                CreatePinStep.Enter, CreatePinStep.Confirm -> PinStepContent(state, onAction, contentModifier)
                CreatePinStep.Biometric -> EnableBiometricContent(state.stepCount, contentModifier)
            }
        }
    }
}

@Composable
private fun PinStepContent(
    state: CreatePinUiState,
    onAction: (CreatePinAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConfirm = state.step == CreatePinStep.Confirm
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingHeading(
            step = state.step.ordinal + 1,
            stepCount = state.stepCount,
            title = stringResource(if (isConfirm) R.string.applock_confirm_title else R.string.applock_create_title),
            message = pluralStringResource(
                if (isConfirm) R.plurals.applock_confirm_message else R.plurals.applock_create_message,
                PinRules.LENGTH,
                PinRules.LENGTH,
            ),
            modifier = Modifier.padding(top = 40.dp),
        )
        PinDots(
            length = state.inputLength,
            status = when (state.match) {
                PinMatch.Match -> PinDotsStatus.Success
                PinMatch.Mismatch -> PinDotsStatus.Error
                PinMatch.Unknown -> PinDotsStatus.Typing
            },
            modifier = Modifier.padding(top = 36.dp, bottom = 12.dp),
        )
        PinStatusText(isConfirm = isConfirm, match = state.match)
        PinPad(
            onDigit = { onAction(CreatePinAction.Digit(it)) },
            onDelete = { onAction(CreatePinAction.Delete) },
            isDeleteEnabled = state.inputLength > 0,
            enabled = !state.isSaving,
            modifier = Modifier.padding(top = 28.dp, bottom = 16.dp),
        )
    }
}

/** The hint under the dots on Create; on Confirm, whether the PINs match (with the color, in words). */
@Composable
private fun PinStatusText(isConfirm: Boolean, match: PinMatch, modifier: Modifier = Modifier) {
    val (text, color) = when {
        !isConfirm -> stringResource(R.string.applock_create_hint) to MaterialTheme.colorScheme.onSurfaceVariant
        match == PinMatch.Match -> stringResource(R.string.applock_pins_match) to MaterialTheme.statusColors.success
        match == PinMatch.Mismatch -> stringResource(R.string.applock_pins_mismatch) to MaterialTheme.colorScheme.error
        else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        color = color,
        style = if (isConfirm) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = modifier
            .heightIn(min = 20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun OnboardingButtons(
    state: CreatePinUiState,
    onAction: (CreatePinAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state.step) {
            CreatePinStep.Enter -> PrimaryButton(
                text = stringResource(R.string.applock_continue),
                onClick = { onAction(CreatePinAction.Continue) },
                enabled = state.inputLength == PinRules.LENGTH,
            )

            CreatePinStep.Confirm -> PrimaryButton(
                text = stringResource(R.string.applock_continue),
                onClick = { onAction(CreatePinAction.Continue) },
                enabled = state.match == PinMatch.Match && !state.isSaving,
            )

            CreatePinStep.Biometric -> EnableBiometricButtons(
                isSaving = state.isSaving,
                onEnable = { onAction(CreatePinAction.EnableBiometric) },
                onSkip = { onAction(CreatePinAction.SkipBiometric) },
            )
        }
    }
}

private fun previewState(step: CreatePinStep, inputLength: Int = 3, match: PinMatch = PinMatch.Unknown) =
    CreatePinUiState(
        step = step,
        stepCount = 3,
        inputLength = inputLength,
        match = match,
        isSaving = false,
        biometricRequest = null,
        effect = null,
        navigation = null,
    )

@PreviewLightDark
@PreviewFontScale
@Composable
private fun CreatePinEnterPreview() {
    CreatePinScreen(state = previewState(CreatePinStep.Enter), onAction = {}, onPinCreated = {}, onBackToSignIn = {})
}

@PreviewLightDark
@Composable
private fun CreatePinMatchPreview() {
    CreatePinScreen(
        state = previewState(CreatePinStep.Confirm, inputLength = 6, match = PinMatch.Match),
        onAction = {},
        onPinCreated = {},
        onBackToSignIn = {},
    )
}

@PreviewLightDark
@Composable
private fun CreatePinMismatchPreview() {
    CreatePinScreen(
        state = previewState(CreatePinStep.Confirm, inputLength = 6, match = PinMatch.Mismatch),
        onAction = {},
        onPinCreated = {},
        onBackToSignIn = {},
    )
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun CreatePinBiometricPreview() {
    CreatePinScreen(state = previewState(CreatePinStep.Biometric), onAction = {
    }, onPinCreated = {}, onBackToSignIn = {})
}
