# MVI base types (`:common:ui`)

Rules: `.claude/rules/ui-mvi.md`. Once these exist in `:common:ui`, the source files win.

## `UiEffectQueue` and `BaseViewModel`

```kotlin
@Stable
class UiEffectQueue<A : UiAction> {
    private val mutex = Mutex()
    private var effects by mutableStateOf(persistentListOf<UiEffect<A>>())

    val current: UiEffect<A>? get() = effects.firstOrNull()

    suspend fun send(effect: UiEffect<A>) = mutex.withLock {
        effects = effects.add(effect)
    }

    suspend fun consume(effect: UiEffect<A>) = mutex.withLock {
        if (effects.firstOrNull() === effect) effects = effects.removeAt(0)
    }
}

abstract class BaseViewModel<S : UiState, A : UiAction, N : UiNavigation> : ViewModel() {

    private val effects = UiEffectQueue<A>()
    private var navigation by mutableStateOf<N?>(null)

    /** The effect waiting to be shown; read it inside [uiState]. */
    protected val currentEffect: UiEffect<A>? get() = effects.current

    /** The navigation waiting to happen; read it inside [uiState]. */
    protected val currentNavigation: N? get() = navigation

    @Composable
    abstract fun uiState(): S

    abstract fun onAction(action: A)

    protected fun sendEffect(effect: UiEffect<A>) {
        viewModelScope.launch { effects.send(effect) }
    }

    protected fun consumeEffect(effect: UiEffect<A>) {
        viewModelScope.launch { effects.consume(effect) }
    }

    /** Main thread only, like every other snapshot field. */
    protected fun navigate(target: N) {
        navigation = target
    }

    protected fun consumeNavigation(target: N) {
        if (navigation === target) navigation = null
    }
}
```

## `UiEffect` and `UiEffectHandler`

```kotlin
// Plain classes (not data classes) on purpose: identity equality, so the same message sent
// twice in a row is shown twice.
sealed interface UiEffect<out A : UiAction> {
    class ShowToast(
        val message: UiText,
        val isLong: Boolean = false,
    ) : UiEffect<Nothing>

    class ShowSnackbar<out A : UiAction>(
        val message: UiText,
        val actionLabel: UiText? = null,
        val action: A? = null,                 // dispatched back to the ViewModel when tapped
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : UiEffect<A>
}

@Composable
fun <A : UiAction> UiEffectHandler(
    effect: UiEffect<A>?,
    snackbarHostState: SnackbarHostState,
    onAction: (A) -> Unit,
    onEffectConsumed: (UiEffect<A>) -> Unit,
) {
    val context = LocalContext.current
    val currentOnAction by rememberUpdatedState(onAction)
    val currentOnEffectConsumed by rememberUpdatedState(onEffectConsumed)

    LaunchedEffect(effect) {
        when (effect) {
            null -> return@LaunchedEffect
            is UiEffect.ShowToast -> Toast.makeText(
                context.applicationContext,            // never keep the Activity alive
                effect.message.asString(context),
                if (effect.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            ).show()
            is UiEffect.ShowSnackbar -> {
                val result = snackbarHostState.showSnackbar(
                    message = effect.message.asString(context),
                    actionLabel = effect.actionLabel?.asString(context),
                    duration = effect.duration,
                )
                if (result == SnackbarResult.ActionPerformed) effect.action?.let(currentOnAction)
            }
        }
        currentOnEffectConsumed(effect)
    }
}
```

## `NavigationHandler`

```kotlin
@Composable
fun <N : UiNavigation> NavigationHandler(
    navigation: N?,
    onNavigate: (N) -> Unit,
    onNavigationConsumed: (N) -> Unit,
) {
    val currentOnNavigate by rememberUpdatedState(onNavigate)
    val currentOnNavigationConsumed by rememberUpdatedState(onNavigationConsumed)

    LaunchedEffect(navigation) {
        navigation ?: return@LaunchedEffect
        currentOnNavigationConsumed(navigation)
        currentOnNavigate(navigation)
    }
}
```
