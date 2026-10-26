---
paths:
  - "common/datastore/**"
  - "feature/*/data/core/**/local/**"
  - "feature/*/data/core/**/di/*DataStore*"
  - "feature/*/data/core/**/di/*Cleaner*"
  - "app/src/main/res/xml/**"
---

# DataStore: one encrypted store per feature

Code: `.claude/reference/datastore.md`. Procedure for a new store: `.claude/skills/add-datastore/`.

## DataStore only (no database)

There is no database right now: `:common:database` (Room) was removed on 2026-10-26 (see the
Database row in `CLAUDE.md` to bring it back). Lists such as the documents
index live in the feature's DataStore too, read whole. Large blobs (document pages) are files,
encrypted with their own keys (`:common:security`), with only their index in DataStore. If a list
grows past a few hundred items or needs queries, that is the time to bring Room back.

## How encryption works

- `:common:datastore` provides `JsonSerializer<T>` (kotlinx.serialization), `LazyAead`, the
  `@DataStoreAead` qualifier and `encryptedDataStore(...)`.
- Encryption uses `androidx.datastore:datastore-tink` (stable since DataStore 1.3.0):
  `AeadSerializer` wraps our JSON serializer and encrypts with a Tink AES-256-GCM keyset. The keyset
  is stored in `datastore_keyset_prefs.xml`, encrypted by a master key in the Android Keystore.
- `LazyAead` defers the slow Keystore setup to the first read/write (always on DataStore's IO scope).

## Rules

- **Typed DataStore only:** one `@Serializable` data class per file, every field with a default.
  Preferences DataStore and `SharedPreferences` are banned (only exception: Tink's keyset prefs
  inside `:common:datastore`).
- **Always encrypted:** create stores only with `encryptedDataStore(...)`, passing the
  `@DataStoreAead Aead`. Never inject or provide an unqualified `Aead`; never call
  `DataStoreFactory`, `DataStore.Builder` or `dataStore(...)` in a feature.
- **Exactly one instance per file:** a `@Singleton` Hilt binding. Two instances on one file throw.
- **Permanent file name:** `<feature>_<name>.json`, unique in the app. It is also the AEAD associated
  data, so renaming makes old data unreadable; change it only with a `DataMigration`.
- **Schema evolution:** add fields with defaults; never change a field's type or meaning in place.
  Removed fields are ignored on read.
- **Corruption resets to defaults:** decryption or parse failures are `CorruptionException`, and
  `ReplaceFileCorruptionHandler` replaces the file with the default value. Only store data the app
  can afford to lose this way (tokens → re-login).
- **Key setup failures are not corruption:** if the Keystore key can't be loaded, `LazyAead` throws
  `IOException` (it converts `GeneralSecurityException` and `ProviderException`). The file is left
  untouched, callers get `AppError.Local.Storage`, and the next access retries. Never catch these and
  reset the store yourself.
- **Backups:** the Keystore key never leaves the device, so exclude the `datastore/` directory and
  `datastore_keyset_prefs.xml` from Auto Backup and device transfer in `data_extraction_rules.xml`
  (and `full_backup_content.xml` for older APIs). Never delete or rename the keyset file or entry.
- Encryption protects data at rest; it does not hide data from code running inside the app. Never
  store secrets that the server should own.
- Pass only the `@ApplicationScope CoroutineScope` (`SupervisorJob() + IO`, process lifetime) to a
  store; never `viewModelScope` or a shorter scope.
- Reads go through `asStoreResult()`, writes through `safeStoreCall { store.updateData { … } }`.
- A feature that stores user data also contributes a `UserDataCleaner` (`@IntoSet`) that resets its
  stores.
