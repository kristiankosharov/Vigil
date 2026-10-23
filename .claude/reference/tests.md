# Test examples

Rules: `.claude/rules/testing.md`.

```kotlin
class UserRepositoryImplTest : FunSpec({
    val api = mockk<UserApi>()
    val dao = mockk<UserDao>(relaxUnitFun = true)
    val repository = UserRepositoryImpl(api, dao)

    afterTest { clearAllMocks() }

    test("refreshUsers returns Timeout when the request times out") {
        coEvery { api.getUsers() } throws HttpRequestTimeoutException("users", 1_000)

        repository.refreshUsers()
            .shouldBeInstanceOf<AppResult.Failure>()
            .error.shouldBeInstanceOf<AppError.Network.Timeout>()
    }

    test("observeUsers emits mapped users") {
        every { dao.observeAll() } returns flowOf(listOf(userEntity))

        repository.observeUsers().test {
            awaitItem() shouldBe AppResult.Success(listOf(userData))
            awaitComplete()
        }
    }
})

class UsersViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()   // wraps a StandardTestDispatcher
    extension(mainDispatcher)

    val deleteUser = mockk<DeleteUserUseCase>()

    // With a StandardTestDispatcher nothing launched in init runs until the scheduler is
    // advanced, so always advance before asserting and read the latest item.

    test("QueryChanged filters users") {
        val viewModel = UsersViewModel(
            savedStateHandle = SavedStateHandle(),
            observeUsers = { flowOf(AppResult.Success(listOf(ann, bob))) },
            refreshUsers = { AppResult.Success(Unit) },
            deleteUser = deleteUser,
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().users.size shouldBe 2

            viewModel.onAction(UsersAction.QueryChanged("ann"))
            awaitItem().users.map { it.name } shouldBe listOf("Ann")
        }
    }

    test("failed refresh shows a retry snackbar, consumed by EffectConsumed") {
        val viewModel = UsersViewModel(
            savedStateHandle = SavedStateHandle(),
            observeUsers = { flowOf(AppResult.Success(emptyList())) },
            refreshUsers = { AppResult.Failure(AppError.Network.NoConnection(null)) },
            deleteUser = deleteUser,
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()   // effects are sent from a launched coroutine
            val snackbar = expectMostRecentItem().effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<UsersAction>>()
            snackbar.action shouldBe UsersAction.Refresh

            viewModel.onAction(UsersAction.EffectConsumed(snackbar))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().effect shouldBe null
        }
    }

    test("Refresh is ignored while a refresh is running") {
        val refreshUsers = mockk<RefreshUsersUseCase>()
        coEvery { refreshUsers() } coAnswers { delay(1_000); AppResult.Success(Unit) }
        val viewModel = UsersViewModel(SavedStateHandle(), { emptyFlow() }, refreshUsers, deleteUser)

        viewModel.onAction(UsersAction.Refresh)   // init already started one
        mainDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { refreshUsers() }
    }
})
```
