---
name: add-datastore
description: Add an encrypted typed DataStore to a feature (serializable model, Hilt provider with encryptedDataStore, repository access, UserDataCleaner, JVM tests). Use when the user asks to persist settings, preferences, flags, tokens, drafts or other small data.
---

# Add an encrypted DataStore to feature `foo`

Rules: `.claude/rules/datastore.md` (check the Room-vs-DataStore table first). Code:
`.claude/reference/datastore.md`.

1. **Model** in `feature/foo/data/core/.../local/`: `@Serializable internal data class FooPreferences(…)`
   with a default on **every** field.
2. **Provider** in `di/FooDataStoreModule.kt` (`internal object`, `SingletonComponent`):
   `@Provides @Singleton` returning `context.encryptedDataStore(fileName = "foo_<name>.json",
   serializer = FooPreferences.serializer(), defaultValue = FooPreferences(), aead = <@DataStoreAead Aead>,
   scope = <@ApplicationScope CoroutineScope>)`. The file name is permanent and unique.
3. **Repository:** reads `store.data.map { … }.distinctUntilChanged().asStoreResult()`; writes
   `safeStoreCall { store.updateData { it.copy(…) } }`. Map to `*Data` models; the model never leaves
   `data:core`.
4. **Cleaner:** if it holds user data, add `di/FooDataCleanerModule.kt` with an `@IntoSet`
   `UserDataCleaner { store.updateData { FooPreferences() } }`.
5. **Backups:** nothing to do per feature — the whole `datastore/` directory is already excluded.
6. **Tests** (JVM, temp file, test `Aead` from a generated Tink keyset): defaults, updates, an old
   file without the new fields gets defaults, a file from another key or file name resets to defaults.
7. **Later changes:** only add fields with defaults; never change a field's type or rename the file
   (use a `DataMigration` if you must).

Never use Preferences DataStore, `SharedPreferences`, `DataStoreFactory`, `DataStore.Builder` or an
unqualified `Aead` in a feature.
