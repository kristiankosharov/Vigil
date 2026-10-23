@Immutable
internal data class BarUiState(
    val isLoading: Boolean,
    val effect: UiEffect<BarAction>?,
) : UiState

internal sealed interface BarAction : UiAction {
    data object Refresh : BarAction
    class EffectConsumed(val effect: UiEffect<BarAction>) : BarAction
}

@HiltViewModel
internal class BarViewModel @Inject constructor(
    private val loadBar: LoadBarUseCase,
) : BaseViewModel<BarUiState, BarAction, Nothing>() {

    private var isLoading by mutableStateOf(false)
    private var loadJob: Job? = null

    init { load() }

    @Composable
    override fun uiState() = BarUiState(isLoading = isLoading, effect = currentEffect)

    override fun onAction(action: BarAction) {
        when (action) {
            BarAction.Refresh -> load()
            is BarAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            isLoading = true
            try {
                loadBar().onFailure { sendEffect(UiEffect.ShowSnackbar(it.toUiText())) }
            } finally {
                isLoading = false
            }
        }
    }
}

@Composable
fun BarRoute(modifier: Modifier = Modifier) {
    val viewModel: BarViewModel = hiltViewModel()
    BarScreen(state = viewModel.uiState(), onAction = viewModel::onAction, modifier = modifier)
}

@Composable
internal fun BarScreen(
    state: BarUiState,
    onAction: (BarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { onAction(BarAction.EffectConsumed(it)) },
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        // content: Modifier.padding(padding), strings from R.string.foo_*, a11y per ui-quality.md
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun BarScreenPreview() {
    AppTheme { // theme from :common:ui
        BarScreen(state = BarUiState(isLoading = false, effect = null), onAction = {})
    }
}
