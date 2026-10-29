---
paths:
  - "**/di/**"
---

# Hilt modules

- Every `@Module` lives in a `di` package, **one module per file, one concern per module**, named
  `<Feature><Concern>Module` (`UsersRepositoryModule`, `UsersDataStoreModule`, …).
- `@Binds` in an `interface`/`abstract class` module; `@Provides` in an `object` module; never both
  in one module.
- Pure Kotlin modules (`domain`, `data:api`, `:common:core`) see only `hilt-core`, so their modules
  are installed in `SingletonComponent`, unscoped. `ViewModelComponent` and other Android
  components exist only in Android modules.
