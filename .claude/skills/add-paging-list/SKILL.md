---
name: add-paging-list
description: Add an offline-first paged list with Paging 3 (Room PagingSource, RemoteMediator, domain mapping, LazyPagingItems in the @Composable uiState, LazyColumn UI, tests). Use when the user asks for an infinite, paged or unbounded list such as a feed, search results or history.
---

# Add a paged list

> **Needs Room, which was removed on 2026-10-26.** Bring `:common:database` back first (Database
> row in `CLAUDE.md`).

Use Paging 3 only for lists that can grow without bound. Small, bounded lists keep the normal
`Flow<AppResult<List<T>>>` pattern.

1. **Room** (`add-room-entity` skill): the item entity, plus a remote-keys entity if the API is
   key-based. The DAO returns `PagingSource<Int, FooEntity>`.
2. **`RemoteMediator`** in `:feature:foo:data:core`, package `…data.paging`: loads pages through
   `safeApiCall`, writes them into Room in a transaction, and returns
   `MediatorResult.Error(AppErrorException(appError))` on failure (`AppErrorException` lives in
   `:common:core`).
3. **Repository** (`data:api` may depend on `androidx.paging:paging-common`): returns
   `Flow<PagingData<FooData>>` from `Pager(config, remoteMediator = …) { dao.pagingSource() }`, mapped
   to `*Data`. Not wrapped in `AppResult`.
4. **Use case:** a SAM one-liner mapping with `PagingData.map { it.toDomain() }`.
5. **ViewModel:** a private flow built once with `.cachedIn(viewModelScope)`; `uiState()` collects it
   with `collectAsLazyPagingItems()`. `LazyPagingItems` is the one non-immutable type allowed in a
   `UiState` (consider `@Stable` instead of `@Immutable` on that state).

   ```kotlin
   private val users: Flow<PagingData<UserItem>> =
       pagedUsers().map { page -> page.map(User::toItem) }.cachedIn(viewModelScope)

   @Composable
   override fun uiState(): UsersUiState = UsersUiState(
       users = users.collectAsLazyPagingItems(),
       effect = currentEffect,
   )
   ```
6. **Screen:** `LazyColumn` with `items(count = items.itemCount, key = items.itemKey { it.id })`;
   loading and error rows for `items.loadState.refresh` / `.append`; retry with `items.retry()`; read
   the `AppError` from `(loadState as LoadState.Error).error as AppErrorException` and show
   `toUiText()`.
7. **Tests:** repository/use case with `paging-testing`'s `asSnapshot()`; the `RemoteMediator` with a
   mocked API and in-memory Room; the screen with a Compose UI test (`collectAsLazyPagingItems()`
   needs Android's main looper, so not in JVM Molecule tests). Run them with
   `scripts/check.sh tests :feature:foo:data:core :feature:foo:domain` and
   `scripts/check.sh android :feature:foo:ui`.
