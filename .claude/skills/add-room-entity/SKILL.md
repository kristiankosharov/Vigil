---
name: add-room-entity
description: Add or change a Room table for a feature in the shared AppDatabase in :common:database (entity, DAO, registration, version bump, migration, migration test). Use when the user asks to store a list or table in the database, add a column, or change a Room schema.
---

# Add or change a Room table for feature `foo`

Rules: `.claude/rules/room.md`. Code: `.claude/reference/room.md`.

1. **Entity** in `common/database/src/main/kotlin/app/vigil/common/database/foo/`:
   `@Entity(tableName = "foo_<name>")` data class, public.
2. **DAO** in the same package: `suspend` writes and one-shot reads, `Flow` for observed reads,
   `@Transaction` for multi-statement writes, no business logic. For Paging, return a
   `PagingSource<Int, FooEntity>`.
3. **Register** the entity in `AppDatabase.entities`, add `abstract fun fooDao(): FooDao`, and
   provide it in `di/DaoModule`.
4. **Version and migration:** bump `AppDatabase.version` by one. Prefer an `AutoMigration(from, to)`
   (with a spec for renames/deletes); otherwise add `Migration_<from>_<to>` in `migrations/` and
   register it in the builder. Never destructive migration in release.
5. **Schema:** build once so the new schema JSON is exported; commit it with the change.
6. **Migration test:** `MigrationTestHelper` test in `common/database/src/androidTest` that migrates
   from the previous version and checks the data.
7. **User data:** if the table holds user data, make sure the database `UserDataCleaner` clears it.
8. **Use it** from `:feature:foo:data:core` only (import only `...database.foo.*`), through
   `safeDbCall` / `asDbResult()`, mapping entities to `*Data` models in the repository.
9. Run `scripts/check.sh tests :common:database :feature:foo:data:core` and, with an emulator
   running, `scripts/check.sh android :common:database` (the migration test is instrumented; if
   no device is connected, say the migration test did not run). Konsist (package ownership rules)
   runs automatically at the end of the turn.

If another branch bumped the version first, re-number your migration to the next free version.
