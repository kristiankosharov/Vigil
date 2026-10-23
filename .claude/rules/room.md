---
paths:
  - "common/database/**"
  - "feature/*/data/core/**/repository/**"
  - "feature/*/data/core/**/paging/**"
---

# Room: one `AppDatabase` in `:common:database`

Code: `.claude/reference/room.md`. Procedure for a new table: `.claude/skills/add-room-entity/`.

- All entities, DAOs, type converters, migrations and the single `@Database` live in
  `:common:database`, in **one package per feature**, named after the feature module
  (`app.vigil.common.database.users`, `….orders`). Root package: `AppDatabase`, `converters/`,
  `migrations/`, `di/` (`DatabaseModule`, `DaoModule`, `DatabaseCleanerModule`).
- Entities and DAOs are `public`, but a package belongs to **one** feature: `:feature:x:data:core`
  imports only `app.vigil.common.database.x.*` plus root helpers. Nothing outside `data:core`
  modules and `:common:database` imports them. `AppDatabase` is used only inside `:common:database`.
  (Konsist enforces all of this.)
- Entities never leave `data:core`; repositories map them to `*Data` models.
- Table names are prefixed with the feature: `@Entity(tableName = "users_user")`.
- Foreign keys and joins across feature packages only between features that already have a
  `data:api` dependency on each other; otherwise store only the other feature's ID.
- DAOs: `suspend` for writes and one-shot reads, `Flow` for observed reads, `@Transaction` for
  multi-statement writes, no business logic.
- **One version number** for the whole database. Prefer `AutoMigration`; manual migrations are
  `Migration_<from>_<to>` in `migrations/`. Export schemas (`room { schemaDirectory(...) }`), commit
  the JSON with the migration, and test every migration with `MigrationTestHelper`. Never use
  destructive migration in release builds. If two branches bump the version, the one merged second
  re-numbers its migration.
- The builder uses `BundledSQLiteDriver` and the injected IO dispatcher as query context, provided
  once as `@Singleton` in `DatabaseModule`. The `vigil.room` convention plugin is applied only to
  `:common:database`.
- Every Room call from a repository goes through `safeDbCall` / `asDbResult()`.
- Trade-off to keep in mind: any entity change recompiles every `data:core` module.
