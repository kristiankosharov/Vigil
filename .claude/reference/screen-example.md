# A full screen

Rules: `.claude/rules/ui-mvi.md`. Procedure: `.claude/skills/new-screen/`.

## UiState

```kotlin
@Immutable
internal data class UsersUiState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val users: ImmutableList<UserItem>,
    val query: String,
    val effect: UiEffect<UsersAction>?,
) : UiState
```

## Actions

```kotlin
internal sealed interface UsersAction : UiAction {
    data object Refresh : UsersAction
    data class QueryChanged(val query: String) : UsersAction
    data class DeleteUser(val id: String) : UsersAction
    class EffectConsumed(val effect: UiEffect<UsersAction>) : UsersAction
}
```

## ViewModel

```kotlin
@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
internal class UsersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeUsers: ObserveUsersUseCase,
    private val refreshUsers: RefreshUsersUseCase,
    private val deleteUser: DeleteUserUseCase,
) : BaseViewModel<UsersUiState, UsersAction, Nothing>() {

    private var isLoading by mutableStateOf(true)
    private var isRefreshing by mutableStateOf(false)
    private var users by mutableStateOf<ImmutableList<UserItem>>(persistentListOf())
    private var query by savedStateHandle.saveable { mutableStateOf("") }   // survives process death
    private var refreshJob: Job? = null

    init {
        observeUsers()
            .onEach { result ->
                isLoading = false
                result
                    .onSuccess { data -> users = data.map(User::toItem).toImmutableList() }
                    .onFailure { appError -> sendEffect(UiEffect.ShowToast(appError.toUiText())) }
            }
            .launchIn(viewModelScope)
        refresh()
    }

    @Composable
    override fun uiState(): UsersUiState {
        val filteredUsers = remember(users, query) {
            users.filter { it.name.contains(query, ignoreCase = true) }.toImmutableList()
        }
        return UsersUiState(
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            users = filteredUsers,
            query = query,
            effect = currentEffect,
        )
    }

    override fun onAction(action: UsersAction) {
        when (action) {
            UsersAction.Refresh -> refresh()
            is UsersAction.QueryChanged -> query = action.query
            is UsersAction.DeleteUser -> delete(action.id)
            is UsersAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            isRefreshing = true
            try {
                refreshUsers().onFailure { appError ->
                    sendEffect(
                        UiEffect.ShowSnackbar(
                            message = appError.toUiText(),
                            actionLabel = UiText.Resource(CommonR.string.common_retry), // import app.vigil.common.ui.R as CommonR
                            action = UsersAction.Refresh,
                        ),
                    )
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            deleteUser(id)
                .onSuccess { sendEffect(UiEffect.ShowToast(UiText.Resource(R.string.users_user_deleted))) }
                .onFailure { appError -> sendEffect(UiEffect.ShowSnackbar(appError.toUiText())) }
        }
    }
}
```

## Route and Screen

```kotlin
@Composable
fun UsersRoute(
    onNavigateToDetails: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: UsersViewModel = hiltViewModel()
    UsersScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onUserClick = onNavigateToDetails,
        modifier = modifier,
    )
}

@Composable
internal fun UsersScreen(
    state: UsersUiState,
    onAction: (UsersAction) -> Unit,
    onUserClick: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { effect -> onAction(UsersAction.EffectConsumed(effect)) },
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        // content
    }
}
```

## ViewModel with arguments (assisted injection) and result navigation

```kotlin
internal sealed interface UserDetailsNavigation : UiNavigation {
    data object Back : UserDetailsNavigation
}

@HiltViewModel(assistedFactory = UserDetailsViewModel.Factory::class)
internal class UserDetailsViewModel @AssistedInject constructor(
    @Assisted private val userId: String,
    private val deleteUser: DeleteUserUseCase,
) : BaseViewModel<UserDetailsUiState, UserDetailsAction, UserDetailsNavigation>() {

    @AssistedFactory
    interface Factory {
        fun create(userId: String): UserDetailsViewModel
    }

    @Composable
    override fun uiState() = UserDetailsUiState(
        /* ... */
        effect = currentEffect,
        navigation = currentNavigation,
    )

    override fun onAction(action: UserDetailsAction) {
        when (action) {
            UserDetailsAction.Delete -> viewModelScope.launch {
                deleteUser(userId)
                    .onSuccess { navigate(UserDetailsNavigation.Back) }
                    .onFailure { appError -> sendEffect(UiEffect.ShowSnackbar(appError.toUiText())) }
            }
            is UserDetailsAction.NavigationConsumed -> consumeNavigation(action.navigation)
            is UserDetailsAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }
}

@Composable
fun UserDetailsRoute(
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<UserDetailsViewModel, UserDetailsViewModel.Factory> { factory ->
        factory.create(userId)
    }
    UserDetailsScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

// inside UserDetailsScreen
NavigationHandler(
    navigation = state.navigation,
    onNavigate = { navigation ->
        when (navigation) {
            UserDetailsNavigation.Back -> onBack()
        }
    },
    onNavigationConsumed = { onAction(UserDetailsAction.NavigationConsumed(it)) },
)
```
