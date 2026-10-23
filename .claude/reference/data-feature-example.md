# A full feature: data:api, data:core, domain

Rules: `.claude/rules/data-layer.md`, `.claude/rules/domain.md`.

## `data:api`

```kotlin
public interface UserRepository {
    public fun observeUsers(): Flow<AppResult<List<UserData>>>
    public suspend fun getUser(id: String): AppResult<UserData>
    public suspend fun refreshUsers(): AppResult<Unit>
}

public data class UserData(val id: String, val name: String, val email: String)
```

## `data:core` layout

```
data/core/src/main/kotlin/app/vigil/feature/users/data/
├── remote/      UserApi (interface), UserApiImpl, UserDto
├── local/       UsersPreferences (DataStore model); DAO/entities come from common.database.users
├── mapper/      UserDto.toEntity(), UserEntity.toData(), UsersPreferences.toData(), ...
├── repository/  UserRepositoryImpl, UsersSettingsRepositoryImpl
└── di/          UsersApiModule, UsersDataStoreModule, UsersRepositoryModule, UsersDataCleanerModule
```

## Repository

```kotlin
internal class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val dao: UserDao,
) : UserRepository {

    override fun observeUsers(): Flow<AppResult<List<UserData>>> =
        dao.observeAll()
            .map { entities -> entities.map(UserEntity::toData) }
            .asDbResult()

    override suspend fun getUser(id: String): AppResult<UserData> =
        when (val result = safeDbCall { dao.getById(id) }) {
            is AppResult.Success -> result.data?.let { AppResult.Success(it.toData()) }
                ?: AppResult.Failure(AppError.Local.NotFound)
            is AppResult.Failure -> result
        }

    override suspend fun refreshUsers(): AppResult<Unit> =
        when (val remote = safeApiCall { api.getUsers() }) {
            is AppResult.Success -> safeDbCall { dao.replaceAll(remote.data.map(UserDto::toEntity)) }
            is AppResult.Failure -> remote
        }
}
```

## Hilt modules (`data:core/di`)

```kotlin
// di/UsersRepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal interface UsersRepositoryModule {
    @Binds fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

// di/UsersApiModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal interface UsersApiModule {
    @Binds fun bindUserApi(impl: UserApiImpl): UserApi
}

// UserDao is provided by :common:database (DaoModule); the DataStore by UsersDataStoreModule.

// di/UsersDataCleanerModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal object UsersDataCleanerModule {
    @Provides @IntoSet
    fun provideUsersDataCleaner(store: DataStore<UsersPreferences>): UserDataCleaner =
        UserDataCleaner { store.updateData { UsersPreferences() } }
}
```

## One-line use cases (SAM)

```kotlin
public fun interface ObserveUsersUseCase {
    public operator fun invoke(): Flow<AppResult<List<User>>>
}

public fun interface RefreshUsersUseCase {
    public suspend operator fun invoke(): AppResult<Unit>
}

// di/UsersUseCaseModule.kt — domain is pure Kotlin (hilt-core only): SingletonComponent, unscoped
@Module
@InstallIn(SingletonComponent::class)
internal object UsersUseCaseModule {
    @Provides
    fun provideObserveUsersUseCase(repository: UserRepository) =
        ObserveUsersUseCase { repository.observeUsers().mapResult { users -> users.map(UserData::toDomain) } }

    @Provides
    fun provideRefreshUsersUseCase(repository: UserRepository) =
        RefreshUsersUseCase(repository::refreshUsers)
}
```

## Use case with logic (`*UseCaseImpl`)

```kotlin
public interface GetActiveUsersUseCase {
    public suspend operator fun invoke(): AppResult<List<User>>
}

internal class GetActiveUsersUseCaseImpl @Inject constructor(
    private val repository: UserRepository,
) : GetActiveUsersUseCase {
    override suspend fun invoke(): AppResult<List<User>> {
        // combine / filter / validate
    }
}

// di/UsersUseCaseBindsModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal interface UsersUseCaseBindsModule {
    @Binds fun bindGetActiveUsersUseCase(impl: GetActiveUsersUseCaseImpl): GetActiveUsersUseCase
}
```
