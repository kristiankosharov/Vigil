# Room: the single AppDatabase

Rules: `.claude/rules/room.md`.

## Layout

```
common/database/src/main/kotlin/app/vigil/common/database/
├── AppDatabase.kt
├── converters/   InstantConverter, ...
├── migrations/   Migration_3_4.kt, ...
├── users/        UserEntity, UserDao           ← used only by :feature:users:data:core
├── orders/       OrderEntity, OrderDao         ← used only by :feature:orders:data:core
└── di/           DatabaseModule (AppDatabase), DaoModule (every DAO), DatabaseCleanerModule
```

## `AppDatabase`

```kotlin
@Database(
    entities = [UserEntity::class, OrderEntity::class],
    version = 4,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(InstantConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun orderDao(): OrderDao
}
```
