package app.vigil.feature.applock.ui.lock

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * The outermost layer of the app: while the app is locked (or the lock state isn't known yet) the
 * lock screen covers [content], which stays composed underneath so navigation state survives.
 *
 * Background/foreground comes from the process lifecycle (a rotation is not a trip to the
 * background); forwarding those events as actions is this host's only side effect.
 */
@Composable
fun AppLockHost(content: @Composable () -> Unit) {
    val viewModel: AppLockViewModel = hiltViewModel()
    val processLifecycle = ProcessLifecycleOwner.get()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP, processLifecycle) {
        viewModel.onAction(AppLockAction.AppBackgrounded)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START, processLifecycle) {
        viewModel.onAction(AppLockAction.AppForegrounded)
    }
    AppLockLayer(state = viewModel.uiState(), onAction = viewModel::onAction, content = content)
}

@Composable
internal fun AppLockLayer(
    state: AppLockUiState,
    onAction: (AppLockAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val covered = state.status != AppLockStatus.Unlocked
    Box(modifier.fillMaxSize()) {
        // Hidden from accessibility services while covered, so the locked app can't be read out.
        Box(if (covered) Modifier.clearAndSetSemantics {} else Modifier) { content() }
        if (covered) {
            val activity = LocalActivity.current
            // Declared after the content, so it wins over the app's own back handling.
            BackHandler { activity?.moveTaskToBack(true) }
            AppLockScreen(state = state, onAction = onAction, modifier = Modifier.fillMaxSize())
        }
    }
}
