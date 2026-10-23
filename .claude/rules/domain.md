---
paths:
  - "feature/*/domain/**"
---

# Domain layer: use cases

Code: `.claude/reference/data-feature-example.md` (use case section).

- A deliberately **thin** layer: use cases and domain models only. Domain models are what the UI
  sees; they are mapped from `*Data` models in `internal` mappers (`UserData.toDomain()`).
- Use cases depend **only** on repository interfaces from `data:api`. Never on Android framework
  classes, Ktor, Room, or another feature's domain.
- One use case = one action, named `VerbNounUseCase`, invoked through `operator fun invoke`.
- Every use case is a public **interface** named `*UseCase`.
- **One-line use cases** (a single expression that delegates to a repository, optionally mapping the
  result) **must** be a SAM `fun interface`, provided as a lambda from an `object` Hilt module
  (`UsersUseCaseModule`). Do not create an `*Impl` class for them.
- **Use cases with real logic**: `*UseCase` interface + `internal class *UseCaseImpl @Inject`,
  bound with `@Binds` in a separate interface module (`UsersUseCaseBindsModule`).
- Use case Hilt modules are installed in `SingletonComponent` with **no scope annotation**: this is
  a pure Kotlin module, so it sees only `hilt-core` and `ViewModelComponent` doesn't exist here.
  Unscoped bindings give every ViewModel its own instance. Never make a use case `@Singleton` or
  give it state.
- Paging: map items with `PagingData.map { it.toDomain() }`; that is still a SAM one-liner.
- Pure Kotlin module: no logging (return `AppResult`; the Android caller logs if needed).
